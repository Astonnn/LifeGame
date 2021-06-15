package application;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LifeUniverse {
	private static final double LOAD_FACTOR = .9;
	private static final int INITIAL_SIZE = 16;
	private static final int HASHMAP_LIMIT = 24;

	private static final int MASK_LEFT = 1;
	private static final int MASK_TOP = 2;
	private static final int MASK_RIGHT = 4;
	private static final int MASK_BOTTOM = 8;

	private int last_id;
	private int hashmap_size;
	private int max_load;

	private List<TreeNode> hashmap;

	private List<TreeNode> empty_tree_cache;
	private List<TreeNode> level2_cache;

	private double[] _powers;

	private byte[] _bitcounts;

	public int rule_b;
	public int rule_s;

	public TreeNode root;
	// public TreeNode root(){return root == null ? TreeNode.True : root;}

	public TreeNode rewind_state;

	int step;
	int generation;

	private TreeNode false_leaf;
	private TreeNode true_leaf;

	public LifeUniverse() {
		this.last_id = 0;
		this.hashmap_size = 0;
		this.max_load = 0;

		this.hashmap = new ArrayList<>();

		this.empty_tree_cache = new ArrayList<>();
		this.level2_cache = new ArrayList<>();

		this._powers = new double[1024];
		this._powers[0] = 1;

		for (int i = 1; i < 1024; i++) {
			this._powers[i] = this._powers[i - 1] * 2;
		}

		// this._bitcounts = new byte[0x758];
		// this._bitcounts.set(new byte[]{0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2,
		// 3, 3, 4});
		this._bitcounts = Arrays.copyOf(new byte[] { 0, 1, 1, 2, 1, 2, 2, 3, 1,
				2, 2, 3, 2, 3, 3, 4 }, 0x758);
		// System.out.println(Arrays.toString(_bitcounts));
		for (int i = 0x10; i < 0x758; i++) {
			this._bitcounts[i] = (byte) (this._bitcounts[i & 0xF]
					+ this._bitcounts[i >> 4 & 0xF] + this._bitcounts[i >> 8]);
		}
		// System.out.println(Arrays.toString(_bitcounts));

		this.rule_b = 1 << 3;
		this.rule_s = 1 << 2 | 1 << 3;

		this.root = null;
		this.rewind_state = null;

		this.step = 0;
		this.generation = 0;

		this.false_leaf = new TreeNode(3, 0, 0);
		this.true_leaf = new TreeNode(2, 1, 0);

		this.clear_pattern();
	}

	public double pow2(int x) {
		if (x >= 1024) {
			return Double.POSITIVE_INFINITY;
		}
		return this._powers[x];
	}

	public void save_rewind_state() {
		this.rewind_state = this.root;
	}

	public void restore_rewind_state() {
		this.generation = 0;
		this.root = this.rewind_state;

		this.garbage_collect();
	}

	public int eval_mask(int bitmask) {
		int rule = (bitmask & 32) != 0 ? this.rule_s : this.rule_b;

		return rule >> this._bitcounts[bitmask & 0x757] & 1;
	}

	public TreeNode level1_create(int bitmask) {
		return this.create_tree((bitmask & 1) != 0 ? this.true_leaf
				: this.false_leaf, (bitmask & 2) != 0 ? this.true_leaf
				: this.false_leaf, (bitmask & 4) != 0 ? this.true_leaf
				: this.false_leaf, (bitmask & 8) != 0 ? this.true_leaf
				: this.false_leaf);
	}

	public void set_bit(double x, double y, boolean living) {
		// int level = this.get_level_from_bounds({ x: x, y: y });
		int level = this.get_level_from_bounds(new double[] { x, y });

		if (living) {
			while (level > this.root.level) {
				this.root = this.expand_universe(this.root);
			}
		} else {
			if (level > this.root.level) {
				return;
			}
		}
		this.root = this.node_set_bit(this.root, x, y, living);
	}

	public boolean get_bit(double x, double y) {
		// int level = this.get_level_from_bounds({ x: x, y: y });
		int level = this.get_level_from_bounds(new double[] { x, y });
		if (level > this.root.level) {
			return false;
		} else {
			return this.node_get_bit(this.root, x, y);
		}
	}

	public double[] get_root_bounds() {
		if (this.root.population == 0) {
			// return {
			// top: 0,
			// left: 0,
			// bottom: 0,
			// right: 0,
			// };
			return new double[] { 0, 0, 0, 0 };
		}
		// double[] bounds = {
		// top: Double.POSITIVE_INFINITY,
		// left: Double.POSITIVE_INFINITY,
		// bottom: Double.NEGATIVE_INFINITY,
		// right: Double.NEGATIVE_INFINITY,
		// };
		double[] bounds = { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
				Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, };
		double offset = this.pow2(this.root.level - 1);
		this.node_get_boundary(this.root, -offset, -offset, MASK_TOP
				| MASK_LEFT | MASK_BOTTOM | MASK_RIGHT, bounds);
		return bounds;
	}

	public TreeNode empty_tree(int level) {
		// while (this.empty_tree_cache.size() <= level) {
		// this.empty_tree_cache.add(null);
		// }
		if (level < this.empty_tree_cache.size()
				&& this.empty_tree_cache.get(level) != null) {
			return this.empty_tree_cache.get(level);
		}

		TreeNode t;

		if (level == 1) {
			t = this.false_leaf;
		} else {
			t = this.empty_tree(level - 1);
		}

		while (this.empty_tree_cache.size() <= level)
			this.empty_tree_cache.add(null);

		// System.out.println("empty_tree_cache"
		// + Arrays.toString(empty_tree_cache.toArray()) + level);
		// System.out.println("t" + t + this.create_tree(t, t, t, t));
		this.empty_tree_cache.set(level, this.create_tree(t, t, t, t));
		return this.create_tree(t, t, t, t);
	}

	private TreeNode expand_universe(TreeNode node) {
		TreeNode t = this.empty_tree(node.level - 1);

		return this.create_tree(this.create_tree(t, t, t, node.nw),
				this.create_tree(t, t, node.ne, t),
				this.create_tree(t, node.sw, t, t),
				this.create_tree(node.se, t, t, t));
	}

	private void uncache(boolean also_quick) {
		for (int i = 0; i <= this.hashmap_size; i++) {
			TreeNode node = this.hashmap.get(i);

			if (node != null) {
				node.cache = null;
				node.hashmap_next = null;

				if (also_quick) {
					node.quick_cache = null;
				}
			}
		}
	}

	private boolean in_hashmap(TreeNode n) {
		int hash = this.calc_hash(n.nw.id, n.ne.id, n.sw.id, n.se.id)
				& this.hashmap_size;
		TreeNode node = this.hashmap.get(hash);

		for (;;) {
			if (node == null) {
				return false;
			} else if (node == n) {
				return true;
			}

			node = node.hashmap_next;
		}
	}

	private void hashmap_insert(TreeNode n) {
		int hash = this.calc_hash(n.nw.id, n.ne.id, n.sw.id, n.se.id)
				& this.hashmap_size;
		TreeNode node = this.hashmap.get(hash), prev = null;

		for (;;) {
			if (node == null) {
				if (prev != null) {
					prev.hashmap_next = n;
				} else {
					this.hashmap.set(hash, n);
				}

				return;
			}

			prev = node;
			node = node.hashmap_next;
		}
	}

	private TreeNode create_tree(TreeNode nw, TreeNode ne, TreeNode sw,
			TreeNode se) {
		// System.out.println(nw);
		// System.out.println(ne);
		// System.out.println(sw);
		// System.out.println(se);
		int hash = this.calc_hash(nw.id, ne.id, sw.id, se.id)
				& this.hashmap_size;
		TreeNode node = this.hashmap.get(hash), prev = null;

		for (;;) {
			if (node == null) {
				if (this.last_id > this.max_load) {
					this.garbage_collect();
					return this.create_tree(nw, ne, sw, se);
				}

				// TreeNode new_node = new this.TreeNode(nw, ne, sw, se,
				// this.last_id++);
				TreeNode new_node = new TreeNode(nw, ne, sw, se, this.last_id++);

				if (prev != null) {
					prev.hashmap_next = new_node;
				} else {
					this.hashmap.set(hash, new_node);
				}

				return new_node;
			} else if (node.nw == nw && node.ne == ne && node.sw == sw
					&& node.se == se) {
				return node;
			}

			prev = node;
			node = node.hashmap_next;
		}
	}

	void next_generation(boolean is_single) {
		TreeNode root = this.root;

		while ((is_single && root.level <= this.step + 2)
				|| (root.nw == null ? 0 : root.nw.population) != ((root.nw == null || root.nw.se == null) ? 0
						: root.nw.se.se.population)
				|| (root.ne == null ? 0 : root.ne.population) != ((root.ne == null || root.ne.sw == null) ? 0
						: root.ne.sw.sw.population)
				|| (root.sw == null ? 0 : root.sw.population) != ((root.sw == null || root.sw.ne == null) ? 0
						: root.sw.ne.ne.population)
				|| (root.se == null ? 0 : root.se.population) != ((root.se == null || root.se.nw == null) ? 0
						: root.se.nw.nw.population)) {
			root = this.expand_universe(root);
		}

		if (is_single) {
			this.generation += this.pow2(this.step);
			root = this.node_next_generation(root);
		} else {
			this.generation += this.pow2(this.root.level - 2);
			root = this.node_quick_next_generation(root);
		}

		this.root = root;
	}

	private void garbage_collect() {
		if (this.hashmap_size < (1 << HASHMAP_LIMIT) - 1) {
			this.hashmap_size = this.hashmap_size << 1 | 1;
			this.hashmap = new ArrayList<>();
		}

		// this.max_load = this.hashmap_size * LOAD_FACTOR | 0;
		this.max_load = (int) (this.hashmap_size * LOAD_FACTOR);

		while (this.hashmap.size() <= this.hashmap_size) {
			this.hashmap.add(null);
		}

		for (int i = 0; i <= this.hashmap_size; i++)
			this.hashmap.set(i, null);

		this.last_id = 4;
		this.node_hash(this.root);
	}

	private int calc_hash(int nw_id, int ne_id, int sw_id, int se_id) {
		int hash = ((nw_id * 23 ^ ne_id) * 23 ^ sw_id) * 23 ^ se_id;
		return hash;
	}

	public void clear_pattern() {
		this.last_id = 4;
		this.hashmap_size = (1 << INITIAL_SIZE) - 1;
		// this.max_load = this.hashmap_size * LOAD_FACTOR | 0;
		this.max_load = (int) (this.hashmap_size * LOAD_FACTOR);
		this.hashmap = new ArrayList<>();
		this.empty_tree_cache = new ArrayList<>();
		// this.level2_cache = Array(0x10000);
		this.level2_cache = new ArrayList<>();
		// System.out.println(level2_cache.size());
		for (int i = 0; i < 0x10000; i++) {
			this.level2_cache.add(null);
		}
		// System.out.println(level2_cache.size());

		while (this.hashmap.size() <= this.hashmap_size) {
			this.hashmap.add(null);
		}
		// System.out.println(hashmap.size());
		// for (int i = 0; i <= this.hashmap_size; i++)
		// this.hashmap.set(i, null);

		this.root = this.empty_tree(3);
		this.generation = 0;
		// System.out.println(max_load);
		// System.out.println("clear_pattern:this.root.id=" + this.root.id);
	}

	double[] get_bounds(int[] field_x, int[] field_y) {
		if (field_x.length == 0) {
			// return {
			// top: 0,
			// left: 0,
			// bottom: 0,
			// right: 0
			// };
			return new double[] { 0, 0, 0, 0 };
		}
		double[] bounds = { field_y[0], field_x[0], field_y[0], field_x[0] };
		int len = field_x.length;
		for (int i = 1; i < len; i++) {
			int x = field_x[i];
			int y = field_y[i];

			if (x < bounds[1]) {
				bounds[1] = x;
			} else if (x > bounds[3]) {
				bounds[3] = x;
			}

			if (y < bounds[0]) {
				bounds[0] = y;
			} else if (y > bounds[2]) {
				bounds[2] = y;
			}
		}
		return bounds;
	}

	public int get_level_from_bounds(double[] bounds) {
		double max = 4;
		// keys = Object.keys(bounds);

		for (int i = 0; i < bounds.length; i++) {
			// int coordinate = bounds[keys[i]];
			double coordinate = bounds[i];

			if (coordinate + 1 > max) {
				max = coordinate + 1;
			} else if (-coordinate > max) {
				max = -coordinate;
			}
		}

		return (int) Math.ceil(Math.log(max) / Math.log(2)) + 1;
	}

	public TreeNode field2tree(Point[] field, int level) {
		TreeNode tree = TreeNode.make_node();
		int len = field.length;
		for (int i = 0; i < len; i++) {
			int x = field[i].x;
			int y = field[i].y;
			TreeNode node = tree;

			for (int j = level - 2; j >= 0; j--) {
				double offset = this.pow2(j);

				if (x < 0) {
					x += offset;
					if (y < 0) {
						y += offset;
						if (node.nw == null) {
							node.nw = TreeNode.make_node();
						}
						node = node.nw;
					} else {
						y -= offset;
						if (node.sw == null) {
							node.sw = TreeNode.make_node();
						}
						node = node.sw;
					}
				} else {
					x -= offset;
					if (y < 0) {
						y += offset;
						if (node.ne == null) {
							node.ne = TreeNode.make_node();
						}
						node = node.ne;
					} else {
						y -= offset;
						if (node.se == null) {
							node.se = TreeNode.make_node();
						}
						node = node.se;
					}
				}
			}

			if (x < 0) {
				if (y < 0) {
					// node.nw = true;
					node.nw = TreeNode.True;
				} else {
					// node.sw = true;
					node.sw = TreeNode.True;
				}
			} else {
				if (y < 0) {
					// node.ne = true;
					node.ne = TreeNode.True;
				} else {
					// node.se = true;
					node.se = TreeNode.True;
				}
			}
		}
		return tree;
	}

	public void make_center(int[] field_x, int[] field_y, double[] bounds) {
		int offset_x = (int) (Math.round((bounds[1] - bounds[3]) / 2) - bounds[1]);
		int offset_y = (int) (Math.round((bounds[0] - bounds[2]) / 2) - bounds[0]);

		this.move_field(field_x, field_y, offset_x, offset_y);

		bounds[1] += offset_x;
		bounds[3] += offset_x;
		bounds[0] += offset_y;
		bounds[2] += offset_y;
	}

	public void move_field(int[] field_x, int[] field_y, int offset_x,
			int offset_y) {
		int len = field_x.length;

		for (int i = 0; i < len; i++) {
			field_x[i] += offset_x;
			field_y[i] += offset_y;
		}
	}

	public void setup_field(int[] field_x, int[] field_y, double[] bounds) {
		if (bounds == null) {
			bounds = this.get_bounds(field_x, field_y);
		}

		int level = this.get_level_from_bounds(bounds);
		int offset = (int) this.pow2(level - 1);
		int count = field_x.length;
		// System.out.println("level" + level);
		// System.out.println("offset" + offset);
		// System.out.println("count" + count);

		// System.out.println("field_x" + Arrays.toString(field_x));
		// System.out.println("field_y" + Arrays.toString(field_y));
		this.move_field(field_x, field_y, offset, offset);
		// System.out.println("move_fieldfield_x" + Arrays.toString(field_x));
		// System.out.println("move_fieldfield_y" + Arrays.toString(field_y));

		this.root = this.setup_field_recurse(0, count - 1, field_x, field_y,
				level);
	}

	public int partition(int start, int end, int[] test_field,
			int[] other_field, int offset) {
		int i = start, j = end, swap;
		while (i <= j) {
			while (i <= end && (test_field[i] & offset) == 0) {
				i++;
			}

			while (j > start && (test_field[j] & offset) != 0) {
				j--;
			}

			if (i >= j) {
				break;
			}
			swap = test_field[i];
			test_field[i] = test_field[j];
			test_field[j] = swap;

			swap = other_field[i];
			other_field[i] = other_field[j];
			other_field[j] = swap;

			i++;
			j--;
		}

		return i;
	}

	public TreeNode setup_field_recurse(int start, int end, int[] field_x,
			int[] field_y, int level) {
		if (start > end) {
			return this.empty_tree(level);
		}

		if (level == 2) {
			return this.level2_setup(start, end, field_x, field_y);
		}

		level--;

		int offset = 1 << level;

		int part3 = this.partition(start, end, field_y, field_x, offset);
		int part2 = this.partition(start, part3 - 1, field_x, field_y, offset);
		int part4 = this.partition(part3, end, field_x, field_y, offset);

		System.out.println(start + " " + part2 + " " + part3 + " " + part4
				+ " " + end + " " + level);
		return this.create_tree(this.setup_field_recurse(start, part2 - 1,
				field_x, field_y, level), this.setup_field_recurse(part2,
				part3 - 1, field_x, field_y, level), this.setup_field_recurse(
				part3, part4 - 1, field_x, field_y, level), this
				.setup_field_recurse(part4, end, field_x, field_y, level));
	}

	public TreeNode level2_setup(int start, int end, int[] field_x,
			int[] field_y) {
		int set = 0, x, y;

		for (int i = start; i <= end; i++) {
			x = field_x[i];
			y = field_y[i];

			set |= 1 << (x & 1 | (y & 1 | x & 2) << 1 | (y & 2) << 2);

		}

		if (this.level2_cache.get(set) != null) {
			return this.level2_cache.get(set);
		}

		this.level2_cache.set(
				set,
				this.create_tree(this.level1_create(set),
						this.level1_create(set >> 4),
						this.level1_create(set >> 8),
						this.level1_create(set >> 12)));
		return this.level2_cache.get(set);
	}

	public void setup_meta(TreeNode otca_on, TreeNode otca_off, Point[] field,
			double[] bounds) {
		int level = this.get_level_from_bounds(bounds);
		TreeNode node = this.field2tree(field, level);

		// this.root = setup_meta_from_tree(node, level + 11);
		this.root = setup_meta_from_tree(otca_on, otca_off, node, level + 11);

	}

	private TreeNode setup_meta_from_tree(TreeNode otca_on, TreeNode otca_off,
			TreeNode node, int level) {
		if (level == 11) {
			return node != null ? otca_on : otca_off;
		} else if (node == null) {
			TreeNode dead = setup_meta_from_tree(otca_on, otca_off, null,
					level - 1);

			return this.create_tree(dead, dead, dead, dead);
		} else {
			level--;

			return this.create_tree(
					setup_meta_from_tree(otca_on, otca_off, node.nw, level),
					setup_meta_from_tree(otca_on, otca_off, node.ne, level),
					setup_meta_from_tree(otca_on, otca_off, node.sw, level),
					setup_meta_from_tree(otca_on, otca_off, node.se, level));
		}
	}

	public void set_step(int step) {
		if (step != this.step) {
			this.step = step;

			this.uncache(false);
			this.empty_tree_cache = new ArrayList<>();
			// this.level2_cache = Array(0x10000);
			this.level2_cache = new ArrayList<>();
		}
	}

	public void set_rules(int s, int b) {
		if (this.rule_s != s || this.rule_b != b) {
			this.rule_s = s;
			this.rule_b = b;

			this.uncache(true);
			this.empty_tree_cache = new ArrayList<>();
			// this.level2_cache = Array(0x10000);
			this.level2_cache = new ArrayList<>();
		}
	}

	public static class TreeNode {
		public TreeNode nw;
		public TreeNode ne;
		public TreeNode sw;
		public TreeNode se;

		int id;
		int level;

		public int population;

		public TreeNode cache;
		public TreeNode quick_cache;

		public TreeNode hashmap_next;

		public TreeNode(int id, int population, int level) {
			this.id = id;
			this.population = population;
			this.level = level;
		}

		public TreeNode(TreeNode nw, TreeNode ne, TreeNode sw, TreeNode se,
				int id) {
			this.nw = nw;
			this.ne = ne;
			this.sw = sw;
			this.se = se;

			this.id = id;

			this.level = (nw == null ? 0 : nw.level) + 1;
			this.population = (nw == null ? 0 : nw.population)
					+ (ne == null ? 0 : ne.population)
					+ (sw == null ? 0 : sw.population)
					+ (se == null ? 0 : se.population);

			this.cache = null;
			this.quick_cache = null;

			this.hashmap_next = null;

		}

		TreeNode() {
		}

		static TreeNode make_node() {
			return new TreeNode();
		}

		static TreeNode True = new TreeNode();

	}

	public TreeNode node_set_bit(TreeNode node, double x, double y,
			boolean living) {
		if (node.level == 0) {
			return living ? this.true_leaf : this.false_leaf;
		}

		double offset = node.level == 1 ? 0 : this.pow2(node.level - 2);
		TreeNode nw = node.nw, ne = node.ne, sw = node.sw, se = node.se;

		if (x < 0) {
			if (y < 0) {
				nw = this.node_set_bit(nw, x + offset, y + offset, living);
			} else {
				sw = this.node_set_bit(sw, x + offset, y - offset, living);
			}
		} else {
			if (y < 0) {
				ne = this.node_set_bit(ne, x - offset, y + offset, living);
			} else {
				se = this.node_set_bit(se, x - offset, y - offset, living);
			}
		}

		return this.create_tree(nw, ne, sw, se);
	}

	public boolean node_get_bit(TreeNode node, double x, double y) {
		if (node.population == 0) {
			return false;
		}
		if (node.level == 0) {
			return true;
		}

		double offset = node.level == 1 ? 0 : this.pow2(node.level - 2);

		if (x < 0) {
			if (y < 0) {
				return this.node_get_bit(node.nw, x + offset, y + offset);
			} else {
				return this.node_get_bit(node.sw, x + offset, y - offset);
			}
		} else {
			if (y < 0) {
				return this.node_get_bit(node.ne, x - offset, y + offset);
			} else {
				return this.node_get_bit(node.se, x - offset, y - offset);
			}
		}
	}

	public void node_get_field(TreeNode node, double left, double top,
			int[] field) {
		if (node.population == 0) {
			return;
		}

		if (node.level == 0) {
			// field.push({ x: left, y: top });
		} else {
			double offset = this.pow2(node.level - 1);

			this.node_get_field(node.nw, left, top, field);
			this.node_get_field(node.sw, left, top + offset, field);
			this.node_get_field(node.ne, left + offset, top, field);
			this.node_get_field(node.se, left + offset, top + offset, field);
		}
	}

	public TreeNode node_level2_next(TreeNode node) {
		TreeNode nw = node.nw, ne = node.ne, sw = node.sw, se = node.se;
		if (nw == null)
			nw = TreeNode.True;
		if (ne == null)
			ne = TreeNode.True;
		if (sw == null)
			sw = TreeNode.True;
		if (se == null)
			se = TreeNode.True;
		int bitmask = nw.nw.population << 15 | nw.ne.population << 14
				| ne.nw.population << 13 | ne.ne.population << 12
				| nw.sw.population << 11 | nw.se.population << 10
				| ne.sw.population << 9 | ne.se.population << 8
				| sw.nw.population << 7 | sw.ne.population << 6
				| se.nw.population << 5 | se.ne.population << 4
				| sw.sw.population << 3 | sw.se.population << 2
				| se.sw.population << 1 | se.se.population;

		return this.level1_create(this.eval_mask(bitmask >> 5)
				| this.eval_mask(bitmask >> 4) << 1
				| this.eval_mask(bitmask >> 1) << 2
				| this.eval_mask(bitmask) << 3);
	}

	public TreeNode node_next_generation(TreeNode node) {
		if (node.cache != null) {
			return node.cache;
		}

		if (this.step == node.level - 2) {
			return this.node_quick_next_generation(node);
		}

		if (node.level == 2) {
			if (node.quick_cache != null) {
				return node.quick_cache;
			} else {
				return node.quick_cache = this.node_level2_next(node);
			}
		}

		TreeNode nw = node.nw, ne = node.ne, sw = node.sw, se = node.se, n00 = this
				.create_tree((nw == null || nw.nw == null) ? null : nw.nw.se,
						(nw == null || nw.ne == null) ? null : nw.ne.sw,
						(nw == null || nw.sw == null) ? null : nw.sw.ne,
						(nw == null || nw.se == null) ? null : nw.se.nw), n01 = this
				.create_tree((nw == null || nw.ne == null) ? null : nw.ne.se,
						(ne == null || ne.nw == null) ? null : ne.nw.sw,
						(nw == null || nw.se == null) ? null : nw.se.ne,
						(ne == null || ne.sw == null) ? null : ne.sw.nw), n02 = this
				.create_tree((ne == null || ne.nw == null) ? null : ne.nw.se,
						(ne == null || ne.ne == null) ? null : ne.ne.sw,
						(ne == null || ne.sw == null) ? null : ne.sw.ne,
						(ne == null || ne.se == null) ? null : ne.se.nw), n10 = this
				.create_tree((nw == null || nw.sw == null) ? null : nw.sw.se,
						(nw == null || nw.se == null) ? null : nw.se.sw,
						(sw == null || sw.nw == null) ? null : sw.nw.ne,
						(sw == null || sw.ne == null) ? null : sw.ne.nw), n11 = this
				.create_tree((nw == null || nw.se == null) ? null : nw.se.se,
						(ne == null || ne.sw == null) ? null : ne.sw.sw,
						(sw == null || sw.ne == null) ? null : sw.ne.ne,
						(se == null || se.nw == null) ? null : se.nw.nw), n12 = this
				.create_tree((ne == null || ne.sw == null) ? null : ne.sw.se,
						(ne == null || ne.se == null) ? null : ne.se.sw,
						(se == null || se.nw == null) ? null : se.nw.ne,
						(se == null || se.ne == null) ? null : se.ne.nw), n20 = this
				.create_tree((sw == null || sw.nw == null) ? null : sw.nw.se,
						(sw == null || sw.ne == null) ? null : sw.ne.sw,
						(sw == null || sw.sw == null) ? null : sw.sw.ne,
						(sw == null || sw.se == null) ? null : sw.se.nw), n21 = this
				.create_tree((sw == null || sw.ne == null) ? null : sw.ne.se,
						(se == null || se.nw == null) ? null : se.nw.sw,
						(sw == null || sw.se == null) ? null : sw.se.ne,
						(se == null || se.sw == null) ? null : se.sw.nw), n22 = this
				.create_tree((se == null || se.nw == null) ? null : se.nw.se,
						(se == null || se.ne == null) ? null : se.ne.sw,
						(se == null || se.sw == null) ? null : se.sw.ne,
						(se == null || se.se == null) ? null : se.se.nw);

		return node.cache = this
				.create_tree(this.node_next_generation(this.create_tree(n00,
						n01, n10, n11)), this.node_next_generation(this
						.create_tree(n01, n02, n11, n12)), this
						.node_next_generation(this.create_tree(n10, n11, n20,
								n21)), this.node_next_generation(this
						.create_tree(n11, n12, n21, n22)));
	}

	public TreeNode node_quick_next_generation(TreeNode node) {
		if (node.quick_cache != null) {
			return node.quick_cache;
		}

		if (node.level == 2) {
			return node.quick_cache = this.node_level2_next(node);
		}

		TreeNode nw = node.nw, ne = node.ne, sw = node.sw, se = node.se, n00 = this
				.node_quick_next_generation(nw), n01 = this
				.node_quick_next_generation(this.create_tree(nw.ne, ne.nw,
						nw.se, ne.sw)), n02 = this
				.node_quick_next_generation(ne), n10 = this
				.node_quick_next_generation(this.create_tree(nw.sw, nw.se,
						sw.nw, sw.ne)), n11 = this
				.node_quick_next_generation(this.create_tree(nw.se, ne.sw,
						sw.ne, se.nw)), n12 = this
				.node_quick_next_generation(this.create_tree(ne.sw, ne.se,
						se.nw, se.ne)), n20 = this
				.node_quick_next_generation(sw), n21 = this
				.node_quick_next_generation(this.create_tree(sw.ne, se.nw,
						sw.se, se.sw)), n22 = this
				.node_quick_next_generation(se);

		return node.quick_cache = this.create_tree(this
				.node_quick_next_generation(this
						.create_tree(n00, n01, n10, n11)), this
				.node_quick_next_generation(this
						.create_tree(n01, n02, n11, n12)), this
				.node_quick_next_generation(this
						.create_tree(n10, n11, n20, n21)), this
				.node_quick_next_generation(this
						.create_tree(n11, n12, n21, n22)));
	}

	public void node_hash(TreeNode node) {
		if (!this.in_hashmap(node)) {

			node.id = this.last_id++;
			node.hashmap_next = null;

			if (node.level > 1) {
				this.node_hash(node.nw);
				this.node_hash(node.ne);
				this.node_hash(node.sw);
				this.node_hash(node.se);

				if (node.cache != null) {
					this.node_hash(node.cache);
				}
				if (node.quick_cache != null) {
					this.node_hash(node.quick_cache);
				}
			}

			this.hashmap_insert(node);
		}
	}

	public void node_get_boundary(TreeNode node, double left, double top,
			int find_mask, double[] boundary) {
		if (node.population == 0 || find_mask == 0) {
			return;
		}

		if (node.level == 0) {
			if (left < boundary[1])
				boundary[1] = left;
			if (left > boundary[3])
				boundary[3] = left;

			if (top < boundary[0])
				boundary[0] = top;
			if (top > boundary[2])
				boundary[2] = top;
		} else {
			double offset = this.pow2(node.level - 1);

			if (left >= boundary[1] && left + offset * 2 <= boundary[3]
					&& top >= boundary[0] && top + offset * 2 <= boundary[2]) {
				return;
			}

			int find_nw = find_mask, find_sw = find_mask, find_ne = find_mask, find_se = find_mask;

			if (node.nw.population != 0) {
				find_sw &= ~MASK_TOP;
				find_ne &= ~MASK_LEFT;
				find_se &= ~MASK_TOP & ~MASK_LEFT;
			}
			if (node.sw.population != 0) {
				find_se &= ~MASK_LEFT;
				find_nw &= ~MASK_BOTTOM;
				find_ne &= ~MASK_BOTTOM & ~MASK_LEFT;
			}
			if (node.ne.population != 0) {
				find_nw &= ~MASK_RIGHT;
				find_se &= ~MASK_TOP;
				find_sw &= ~MASK_TOP & ~MASK_RIGHT;
			}
			if (node.se.population != 0) {
				find_sw &= ~MASK_RIGHT;
				find_ne &= ~MASK_BOTTOM;
				find_nw &= ~MASK_BOTTOM & ~MASK_RIGHT;
			}

			this.node_get_boundary(node.nw, left, top, find_nw, boundary);
			this.node_get_boundary(node.sw, left, top + offset, find_sw,
					boundary);
			this.node_get_boundary(node.ne, left + offset, top, find_ne,
					boundary);
			this.node_get_boundary(node.se, left + offset, top + offset,
					find_se, boundary);
		}
	}
}