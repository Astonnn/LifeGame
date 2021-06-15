package application;

// import java.awt.Color;
//import java.awt.Point;

//import javafx.scene.Group;
import application.LifeUniverse.TreeNode;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class LifeCanvasDrawer {
	private int canvas_offset_x;
	private int canvas_offset_y;

	private int canvas_width;
	private int canvas_height;

	public Canvas canvas;
	public GraphicsContext context;

	public WritableImage image_data;
	public int[] image_data_data;

	public double border_width;
	public Color cell_color_rgb;

	public LifeCanvasDrawer drawer;

	public double pixel_ratio;

	public String cell_color;
	public String background_color;

	public double cell_width;

	public int cell_border_width;

	public LifeCanvasDrawer() {
		canvas_offset_x = 0;
		canvas_offset_y = 0;
		drawer = this;
		pixel_ratio = 1;
		this.cell_color = null;
		this.background_color = null;

		this.border_width = 0;
	}

	public boolean init(Pane dom_parent) {
		canvas = new Canvas();
		if (canvas == null || canvas.getGraphicsContext2D() == null) {
			return false;
		}
		drawer.canvas = canvas;
		context = canvas.getGraphicsContext2D();
		dom_parent.getChildren().add(canvas);
		return true;
	}

	public void set_size(double width, double height) {
		if (width != canvas_width || height != canvas_height) {
			double factor = 1;
			// if (true) {
			canvas.setWidth(width);
			canvas.setHeight(height);
			factor = 1;// window.devicePixelRatio
			// } else {
			//
			// }
			pixel_ratio = factor;

			canvas.setWidth(Math.round(width * factor));
			canvas.setHeight(Math.round(height * factor));

			canvas_width = (int) canvas.getWidth();
			canvas_height = (int) canvas.getHeight();
			// Origin
			// image_data = context.createImageData(canvas_width,
			// canvas_height);
			// image_data_data = new Int32Array(image_data.data.buffer);
			// 1.
			// System.out.println(canvas_width + ", " + canvas_height);
			// System.out.println(canvas.getLayoutX() + ", " +
			// canvas.getLayoutY());
			image_data_data = new int[canvas_width * canvas_height];

			// System.out.println(image_data_data[0]);
			// Reader reader = new Reader(image_data_data, canvas_width,
			// canvas_height);
			image_data = new WritableImage(canvas_width, canvas_height);
			for (int i = 0; i < width * height; i++) {
				image_data_data[i] = 0xFF << 24;
				// image_data_data[i] = 0xFFCCCCCC;
			}

			// System.out.println(reader.getColor(0, 0)
			// .toString());
			// System.out.println(image_data.getPixelReader().getColor(0, 0)
			// .toString());
		}
	}

	public void draw_node(TreeNode node, double size, double left, double top) {
		if (node.population == 0) {
			return;
		}
		if (left + size + canvas_offset_x < 0
				|| top + size + canvas_offset_y < 0
				|| left + canvas_offset_x >= canvas_width
				|| top + canvas_offset_y >= canvas_height) {
			return;
		}
		if (size <= 1) {
			if (node.population > 0) {
				fill_square((int) Math.floor(left + canvas_offset_x),
						(int) Math.floor(top + canvas_offset_y), 1);
			}
		} else if (node.level == 0) {
			if (node.population > 0) {
				fill_square((int) (left + canvas_offset_x),
						(int) (top + canvas_offset_y), drawer.cell_width);
			}
		} else {
			size /= 2;
			draw_node(node.nw, size, left, top);
			draw_node(node.ne, size, left + size, top);
			draw_node(node.sw, size, left, top + size);
			draw_node(node.se, size, left + size, top + size);

		}
	}

	public void fill_square(int x, int y, double size) {
		double width = size - cell_border_width;
		double height = width;

		if (x < 0) {
			width += x;
			x = 0;
		}
		if (x + width > canvas_width) {
			width = canvas_width - x;
		}
		if (y < 0) {
			height += y;
			y = 0;
		}
		if (y + height > canvas_height) {
			height = canvas_height - y;
		}
		if (width <= 0 || height <= 0) {
			return;
		}

		int pointer = x + y * canvas_width;
		double row_width = canvas_width - width;

		// int color = cell_color_rgb.r | cell_color_rgb.g << 8 |
		// cell_color_rgb.b << 16 | 0xFF << 24;
		int color = (int) (cell_color_rgb.getRed() * 255)
				| (int) (cell_color_rgb.getGreen() * 255) << 8
				| (int) (cell_color_rgb.getBlue() * 255) << 16 | 0xFF << 24;
		// System.out.println("color: " + color);
		// System.out.println("cell_color_rgb: " + cell_color_rgb.toString());
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				image_data_data[pointer] = color;
				pointer++;
			}
			pointer += row_width;
		}
	}

	public void redraw(TreeNode node) {
		// var bg_color_rgb = color2rgb(drawer.background_color);
		// var bg_color_int = bg_color_rgb.r | bg_color_rgb.g << 8 |
		// bg_color_rgb.b << 16 | 0xFF << 24;
		Color bg_color_rgb = color2rgb(drawer.background_color);
		int bg_color_int = (int) (bg_color_rgb.getRed() * 255)
				| (int) (bg_color_rgb.getGreen() * 255) << 8
				| (int) (bg_color_rgb.getBlue() * 255) << 16 | 0xFF << 24;
		// int bg_color_int = bg_color_rgb.getRGB();
		// System.out.println(bg_color_rgb.toString());
		// System.out.println(bg_color_int);

		cell_border_width = (int) (drawer.border_width * drawer.cell_width);
		cell_color_rgb = color2rgb(drawer.cell_color);
		System.out.println("border_width: " + border_width);
		System.out.println("cell_width: " + cell_width);
		// System.out.println("cell_color_rgb: " + cell_color_rgb);

		int count = canvas_width * canvas_height;
		// System.out.println("count: " + count);
		for (int i = 0; i < count; i++) {
			image_data_data[i] = bg_color_int;
		}
		// System.out.println("image_data_data: " +
		// Arrays.toString(image_data_data));
		double size = Math.pow(2, node.level - 1) * drawer.cell_width;
		draw_node(node, 2 * size, -size, -size);
		System.out.println("size" + size);
		// System.out.println("image_data" + image_data);
		image_data.getPixelWriter().setPixels(0, 0, canvas_width,
				canvas_height, PixelFormat.getIntArgbInstance(),
				image_data_data, 0, canvas_width);
		context.drawImage(image_data, 0, 0);

	}

	public void zoom(boolean out, double center_x, double center_y) {
		if (out) {
			canvas_offset_x -= Math.round((canvas_offset_x - center_x) / 2);
			canvas_offset_y -= Math.round((canvas_offset_y - center_y) / 2);

			drawer.cell_width /= 2;
		} else {
			canvas_offset_x += Math.round(canvas_offset_x - center_x);
			canvas_offset_y += Math.round(canvas_offset_y - center_y);

			drawer.cell_width *= 2;
		}
	}

	public void zoom_at(boolean out, double center_x, double center_y) {
		zoom(out, center_x * pixel_ratio, center_y * pixel_ratio);
	}

	public void zoom_centered(boolean out) {
		zoom(out, canvas_width >> 1, canvas_height >> 1);
	}

	public void zoom_to(double level) {
		while (drawer.cell_width > level) {
			zoom_centered(true);
		}
		while (drawer.cell_width * 2 < level) {
			zoom_centered(false);
		}
	}

	public void center_view() {
		canvas_offset_x = canvas_width >> 1;
		canvas_offset_y = canvas_height >> 1;
	}

	public void move(double dx, double dy) {
		canvas_offset_x += Math.round(dx * pixel_ratio);
		canvas_offset_y += Math.round(dy * pixel_ratio);
	}

	public void fit_bounds(double[] bounds) {
		// double width = bounds.right - bounds.left;
		// double height = bounds.bottom - bounds.top;
		double width = bounds[3] - bounds[1];
		double height = bounds[2] - bounds[0];
		double relative_size;
		int x, y;

		// if(isFinite(width) && isFinite(height)){
		if (width != Double.POSITIVE_INFINITY
				&& height != Double.POSITIVE_INFINITY) {
			relative_size = Math.min(16, // maximum cell size
					Math.min(canvas_width / width, // relative width
							canvas_height / height // relative height
					));
			zoom_to(relative_size);

			// x = Math.round(canvas_width / 2 - (bounds.left + width / 2) *
			// drawer.cell_width);
			// y = Math.round(canvas_height / 2 - (bounds.top + height / 2) *
			// drawer.cell_width);
			x = (int) Math.round(canvas_width / 2 - (bounds[1] + width / 2)
					* drawer.cell_width);
			y = (int) Math.round(canvas_height / 2 - (bounds[0] + height / 2)
					* drawer.cell_width);
		} else {
			zoom_to(16);

			x = canvas_width >> 1;
			y = canvas_height >> 1;
		}

		canvas_offset_x = x;
		canvas_offset_y = y;
	}

	public void draw_cell(double x, double y, boolean set) {
		double cell_x = x * drawer.cell_width + canvas_offset_x;
		double cell_y = y * drawer.cell_width + canvas_offset_y;
		double width = Math.ceil(drawer.cell_width)
				- Math.floor(drawer.cell_width * drawer.border_width);
		if (set) {
			context.setFill(Paint.valueOf(drawer.cell_color));
		} else {
			context.setFill(Paint.valueOf(drawer.background_color));
		}
		System.out.println(cell_color + cell_x + cell_y + width + border_width
				+ background_color);
		context.fillRect(cell_x, cell_y, width, width);
	}

	public double[] pixel2cell(double x, double y) {
		return new double[] {
				Math.floor((x * pixel_ratio - canvas_offset_x + drawer.border_width / 2)
						/ drawer.cell_width),
				Math.floor((y * pixel_ratio - canvas_offset_y + drawer.border_width / 2)
						/ drawer.cell_width) };
	}

	public Color color2rgb(String color) {
		if (color.length() == 4) {
			return Color.rgb(
					Integer.parseInt(
							String.valueOf(new char[] { color.charAt(1),
									color.charAt(1) }), 16),
					Integer.parseInt(
							String.valueOf(color.charAt(2))
									+ String.valueOf(color.charAt(2)), 16),
					Integer.parseInt(
							String.valueOf(color.charAt(3))
									+ String.valueOf(color.charAt(3)), 16));
		} else {
			return Color.rgb(Integer.parseInt(color.substring(1, 3), 16),
					Integer.parseInt(color.substring(3, 5), 16),
					Integer.parseInt(color.substring(5, 7), 16));
		}
	}

	// private class Reader implements PixelReader {
	// private int[] image_data_data;
	// private int canvas_width;
	// private int canvas_height;
	//
	// public Reader(int[] image_data_data, int canvas_width, int canvas_height)
	// {
	// this.image_data_data = image_data_data;
	// this.canvas_width = canvas_width;
	// this.canvas_height = canvas_height;
	// }
	//
	// @Override
	// public int getArgb(int x, int y) {
	// // System.out.println(x + ", " + y + "=="
	// // + image_data_data[x + y * canvas_width]);
	// return image_data_data[x + y * canvas_width];
	// }
	//
	// @Override
	// public Color getColor(int x, int y) {
	// int color = image_data_data[x + y * canvas_width];
	// // System.out.println(color + color & 0xFF);
	//
	// return Color.rgb(color & 0xFF, (color >> 8) & 0xFF,
	// (color >> 16) & 0xFF);
	// }
	//
	// @Override
	// public PixelFormat getPixelFormat() {
	// return PixelFormat.createByteIndexedInstance(image_data_data);
	// }
	//
	// @Override
	// public void getPixels(int x, int y, int w, int h,
	// WritablePixelFormat<ByteBuffer> pixelformat, byte[] buffer,
	// int offset, int scanlineStride) {
	//
	// }
	//
	// @Override
	// public void getPixels(int x, int y, int w, int h,
	// WritablePixelFormat<IntBuffer> pixelformat, int[] buffer,
	// int offset, int scanlineStride) {
	// // IntBuffer buf = IntBuffer.allocate(x + y * canvas_width);
	// }
	//
	// @Override
	// // <T extends Buffer>
	// public <T extends Buffer> void getPixels(int x, int y, int w, int h,
	// WritablePixelFormat<T> pixelformat, T buffer, int scanlineStride) {
	//
	// }

	// }
}