package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.util.Date;
import java.util.function.UnaryOperator;

import application.LifeUniverse.TreeNode;

public class Main extends Application {

	private static double DEFAULT_BORDER = 0.25;
	private static int DEFAULT_FPS = 20;

	private String initial_title;

	// String initial_title = document.title;
	// private String initial_description;

	// private Formats.Result current_pattern;

	private class Callback {
		public void function() {
		}
	}

	// private void onstop();
	private Callback onstop;

	private double last_mouse_x;
	private double last_mouse_y;
	private boolean last_mouse;

	private boolean mouse_set;

	// private double cellLength = 5.0f;
	private double stageWidth = 1600.0f;// 1024.0f;
	private double stageHeight = 900.0f;// 576.0f;
	// private double stageX = 0.0f;
	// private double stageY = 0.0f;
	// private double canvasX = 0.0f;
	// private double canvasY = 0.0f;

	private boolean running;
	private int max_fps;
	// private boolean patterns_loaded;

	// private String pattern_path;

	// private boolean loaded;

	private LifeUniverse life;
	private LifeCanvasDrawer drawer;

	// private String[] examples;

	private Button run_button = new Button("Run");
	private Button rewind_button = new Button("Rewind");
	private Button step_button = new Button("Step");
	private Button superstep_button = new Button("Superstep");
	private Button clear_button = new Button("Clear");
	private Button settings_button = new Button("Settings");
	// private Button import_button = new Button("Import");
	// private Button export_button = new Button("Export");
	private Button randomize_button = new Button("Randomize");
	// private Button pattern_button = new Button("Patterns");
	private Button about_button = new Button("About");

	// private Label pattern_name = new Label("Pattern infos");
	private Label label_step = new Label("1");
	private Label label_pop = new Label("0");
	private Label label_gen = new Label("0");
	private Label label_fps = new Label("0");
	private Label label_mou = new Label("0, 0");
	private Label label_zoom = new Label();

	private Button faster_button = new Button("F");

	public Main() {
		initial_title = "Conway's Game of Life";
		// initial_description = "";

		running = false;
		// patterns_loaded = false;
		// pattern_path = "examples/";
		// loaded = false;

		life = new LifeUniverse();
		drawer = new LifeCanvasDrawer();

	}

	// @args
	// step=10 gist=1

	public static void main(String[] args) {
		// query = args;
		launch(args);
	}

	String[] query;

	@Override
	public void start(Stage primaryStage) throws Exception {
		// if (loaded) {
		// return;
		// }
		// loaded = true;
		// initial_description =
		// "A JavaFX version of Conway's Game of Life, based on the Hashlife-algorithm.";
		try {

			BorderPane root = new BorderPane();
			// Group root = new Group();

			FlowPane toolbar = new FlowPane();
			// menu.setMaxWidth(1000);
			// menu.setMaxHeight(10);
			toolbar.setAlignment(Pos.TOP_RIGHT);
			// menu.setPadding(new Insets(0, 0, 0, 0));//top,right,bottom,left
			// menu.setSpacing(10);
			// menu.setEffect(new DropShadow(2, Color.DARKBLUE));

			toolbar.getChildren().addAll(run_button, rewind_button,
					step_button, superstep_button, clear_button,
					settings_button, /* import_button, export_button, */
					randomize_button, /* pattern_button, */about_button);
			// menu.setAlignment(Pos.BOTTOM_RIGHT);
			// root.getChildren().add(menu);

			FlowPane statusbar = new FlowPane();
			statusbar.setAlignment(Pos.BOTTOM_RIGHT);
			// label_step.setPadding(new Insets(5));
			BorderStroke borderStroke = new BorderStroke(null, null,
					Color.BLACK, null, null, null, BorderStrokeStyle.SOLID,
					null, null, BorderWidths.DEFAULT, new Insets(5));
			// pattern_name.setBorder(new Border(borderStroke));
			label_step.setBorder(new Border(borderStroke));
			label_pop.setBorder(new Border(borderStroke));
			label_gen.setBorder(new Border(borderStroke));
			label_fps.setBorder(new Border(borderStroke));
			label_mou.setBorder(new Border(borderStroke));
			label_zoom.setBorder(new Border(borderStroke));
			statusbar.getChildren().addAll(/* pattern_name, */label_step,
					label_pop, label_gen, label_fps, label_mou, label_zoom);

			GridPane overlay = new GridPane();
			overlay.add(faster_button, 0, 0);

			// System.out.println(root.getWidth() + ", " + root.getHeight());
			if (!drawer.init(root)) {
				return;
			}
			init_ui();
			drawer.set_size(stageWidth, stageHeight - 40);
			reset_settings();

			root.setBackground(new Background(new BackgroundFill(Paint
					.valueOf(drawer.background_color), null, null)));
			// if (parameters.get("step") != null && Pattern.matches("^\\d+$",
			// parameters["step"])) {
			// int step_parameter =
			// Math.round(Math.log(Integer.valueOf(parameters["step"])) /
			// Math.LN2);

			// life.set_step(step_parameter);
			// }

			root.setTop(toolbar);
			root.setBottom(statusbar);
			root.setLeft(overlay);

			Canvas canvas = drawer.canvas;
			canvas.setCursor(Cursor.HAND);
			// System.out.println(canvas.getWidth()+", "+canvas.getHeight());
			// System.out.println(root.getWidth()+", "+root.getHeight());

			Scene scene = new Scene(root, stageWidth, stageHeight);
			primaryStage.setScene(scene);
			primaryStage.setTitle(initial_title);
			primaryStage.setWidth(stageWidth);
			primaryStage.setHeight(stageHeight);
			// primaryStage.setIconified(true);
			// primaryStage.getIcons().add(new Image("/res/icon.jpg"));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init_ui() {

		run_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (running) {
					stop(null);
				} else {
					run();
				}
			}
		});
		step_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (!running) {
					step(true);
				}
			}
		});
		superstep_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (!running) {
					step(false);
				}
			}
		});
		clear_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				stop(new Callback() {
					public void function() {
						// set_title();
						// set_text(pattern_name, "");
						// set_query("");

						life.clear_pattern();
						update_hud();

						drawer.center_view();
						drawer.redraw(life.root);
					}
				});
			}
		});
		rewind_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (life.rewind_state != null) {
					stop(new Callback() {
						public void function() {
							life.restore_rewind_state();

							fit_pattern();
							drawer.redraw(life.root);

							update_hud();
						}
					});
				}
			}
		});

		drawer.canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				System.out.println("Hello World!" + event.getSceneX()
						+ event.getButton() + event.getSceneY()
						+ event.getPickResult());
				if (event.isSecondaryButtonDown() || event.isMiddleButtonDown()) {
					System.out.println(drawer.cell_width);
					if (drawer.cell_width >= 1) {
						double[] coords = drawer.pixel2cell(event.getSceneX(),
								event.getSceneY());

						mouse_set = !life.get_bit(coords[0], coords[1]);
						drawer.canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
								do_field_draw);

						do_field_draw(event);
					}
				} else if (event.isPrimaryButtonDown()) {
					last_mouse_x = event.getSceneX();
					last_mouse_y = event.getSceneY();
					last_mouse = true;
					System.out.println("last_mouse_x, last_mouse_y: "
							+ last_mouse_x + ", " + last_mouse_y);

					System.out.println("start" + drawer.cell_width);
					drawer.canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
							do_field_move);

					// TODO need test
					Runnable redraw = new Runnable() {

						@Override
						public void run() {
							if (last_mouse) {
								Platform.runLater(this);
							}

						}
					};
					Thread thread = new Thread(new Runnable() {

						@Override
						public void run() {
							while (last_mouse) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException ex) {
								}

								// UI update is run on the Application thread
								Platform.runLater(redraw);
							}

						}
					});
					thread.setDaemon(true);
					thread.start();
					lazy_redraw(life.root);
					// })();
				}

			}
		});

		drawer.canvas.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {

				System.out.println("Hello World!" + event.getSceneX()
						+ event.getEventType() + event.getX()
						+ event.isPrimaryButtonDown());
				// last_mouse_x = 0;
				// last_mouse_y = 0;
				last_mouse = false;
				drawer.canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED,
						do_field_draw);
				drawer.canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED,
						do_field_move);
			}
		});
		drawer.canvas.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				// System.out.println("Hello World!" + event.getSceneX()
				// + event.getEventType() + event.getX()
				// + event.isPrimaryButtonDown());
				double[] coords = drawer.pixel2cell(event.getSceneX(),
						event.getSceneY());

				set_text(label_mou, coords[0] + ", " + coords[1]);
				// fix_width(label_mou);
			}
		});
		// drawer.canvas.setOnContextMenuRequested(null);
		drawer.canvas.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				event.consume();// TODO ??e.preventDefault();
				drawer.zoom_at(event.getDeltaY() < 0, event.getSceneX(),
						event.getSceneY());
				// drawer.zoom_at((e.wheelDelta || -e.detail) < 0, e.clientX,
				// e.clientY);
				// System.out.println("event.getDeltaY(): " +
				// event.getDeltaY());

				update_hud();
				lazy_redraw(life.root);

			}
		});
		// TODO randomize_button
		randomize_button.setOnAction(new EventHandler<ActionEvent>() {
			double density = 0.5;
			int width = 200;
			int height = 200;

			@Override
			public void handle(ActionEvent arg0) {
				Dialog<ButtonType> dialog = new Dialog<>();
				dialog.setTitle("Random Pattern");
				dialog.setHeaderText("Look, a Random Pattern Dialog");

				dialog.getDialogPane().getButtonTypes()
						.addAll(ButtonType.APPLY, ButtonType.CANCEL);

				GridPane grid = new GridPane();
				TextField randomize_density = new TextField(
						density == 0 ? "0.5" : String.valueOf(density));
				TextField randomize_width = new TextField(String.valueOf(width));
				TextField randomize_height = new TextField(height <= 0 ? "200"
						: String.valueOf(height));

				grid.add(new Label("Density: "), 0, 0);
				grid.add(randomize_density, 1, 0);
				grid.add(new Label("Width: "), 0, 1);
				grid.add(randomize_width, 1, 1);
				grid.add(new Label("Height: "), 0, 2);
				grid.add(randomize_height, 1, 2);

				dialog.getDialogPane().setContent(grid);
				dialog.showAndWait().ifPresent(
						response -> {
							if (response == ButtonType.APPLY) {
								try {
									density = Math.max(0, Math.min(1, Double
											.parseDouble(randomize_density
													.getText())));
									width = Math.max(0, Integer
											.parseUnsignedInt(randomize_width
													.getText()));
									height = Math.max(0, Integer
											.parseUnsignedInt(randomize_height
													.getText()));
								} catch (NumberFormatException ex) {
									ex.printStackTrace();
								}
								stop(new Callback() {
									public void function() {
										life.clear_pattern();

										// Note: Not exact density because some
										// points
										// may be repeated
										int[] field_x = new int[(int) Math
												.round(width * height * density)];
										int[] field_y = new int[field_x.length];

										for (int i = 0; i < field_x.length; i++) {
											field_x[i] = (int) (Math.random() * width);
											field_y[i] = (int) (Math.random() * height);
										}
										// System.out.println(width + height);
										// System.out.println("field_x"
										// + Arrays.toString(field_x));
										// System.out.println(Arrays.toString(field_y));
										double[] bounds = life.get_bounds(
												field_x, field_y);
										// System.out.println(Arrays.toString(bounds));
										life.make_center(field_x, field_y,
												bounds);
										life.setup_field(field_x, field_y,
												bounds);

										life.save_rewind_state();

										fit_pattern();
										lazy_redraw(life.root);

										update_hud();

									}
								});
							}
						});
			}
		});
		// TODO settings_button
		settings_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Dialog<ButtonType> dialog = new Dialog<>();
				dialog.setTitle("Settings");
				dialog.setHeaderText("Look, a Setting Dialog");

				ButtonType resetButtonType = new ButtonType("Reset",
						ButtonData.BACK_PREVIOUS);
				dialog.getDialogPane()
						.getButtonTypes()
						.addAll(ButtonType.APPLY, resetButtonType,
								ButtonType.CANCEL);

				GridPane grid = new GridPane();
				TextField rule = new TextField("23/3");
				rule.setTextFormatter(new TextFormatter<String>(
						new UnaryOperator<TextFormatter.Change>() {
							@Override
							public TextFormatter.Change apply(
									TextFormatter.Change change) {
								System.out.println(change.getText());
								change.setText(change.getText().replaceAll(
										"[^/0-8]", ""));
								return change;
							}
						}));
				TextField maxfps = new TextField(String.valueOf(max_fps));
				TextField gen_step = new TextField(String.valueOf((int) Math
						.pow(2, life.step)));
				System.out.println("bb" + drawer.border_width);
				TextField border_width = new TextField(String
						.valueOf(drawer.border_width));

				grid.add(new Label("Rule: "), 0, 0);
				grid.add(rule, 1, 0);
				grid.add(new Label("Maximum FPS: "), 0, 1);
				grid.add(maxfps, 1, 1);
				grid.add(new Label("Generation step: "), 0, 2);
				grid.add(gen_step, 1, 2);
				grid.add(new Label("Border width: "), 0, 3);
				grid.add(border_width, 1, 3);

				dialog.getDialogPane().setContent(grid);

				dialog.showAndWait().ifPresent(
						response -> {
							System.out.println("result=" + response);
							if (response == resetButtonType) {
								reset_settings();
								lazy_redraw(life.root);
							} else if (response == ButtonType.APPLY) {
								String rule_str = rule.getText();
								String maxfps_str = maxfps.getText();
								String gen_step_str = gen_step.getText();
								String border_width_str = border_width
										.getText().trim();

								int new_gen_step = (int) Math.round(Math
										.log(Integer.valueOf(gen_step_str))
										/ Math.log(2.0f));

								if (new_gen_step <= 0) {
									life.set_step(0);
									set_text(label_step, "1");
								} else {
									life.set_step(new_gen_step);
									set_text(label_step, String.valueOf(Math
											.pow(2, new_gen_step)));
								}

								max_fps = Integer.valueOf(maxfps_str);
								if (max_fps <= 0) {
									max_fps = DEFAULT_FPS;
								}
								System.out.println(border_width_str
										+ Double.valueOf(border_width_str)
										+ drawer.border_width);
								drawer.border_width = Double
										.valueOf(border_width_str);
								if (drawer.border_width < 0
										|| drawer.border_width > .5) {
									drawer.border_width = DEFAULT_BORDER;
								}
								System.out.println(border_width_str
										+ Double.valueOf(border_width_str)
										+ drawer.border_width);

								lazy_redraw(life.root);
							}
						});
			}
		});

		about_button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("About");
				alert.setHeaderText("Conway's Game of Life in JavaFX");
				alert.setContentText("This is an implementation of Conway's Game of Life or more precisely, the super-fast Hashlife algorithm, written in JavaFX using the Canvas.");

				alert.showAndWait();
			}
		});
	}

	public void stop(Callback callback) {
		if (running) {
			running = false;
			set_text(run_button, "Run");

			onstop = callback;
		} else {
			if (callback != null) {
				callback.function();
			}
		}
	}

	private void reset_settings() {
		drawer.background_color = "#f4f4f4";
		drawer.cell_color = "#000000";

		drawer.border_width = DEFAULT_BORDER;
		drawer.cell_width = 2;

		life.rule_b = 1 << 3;
		life.rule_s = 1 << 2 | 1 << 3;
		life.set_step(0);
		set_text(label_step, "1");

		max_fps = DEFAULT_FPS;

		set_text(label_zoom, "1:2");

		drawer.center_view();
	}

	private void fit_pattern() {
		double[] bounds = life.get_root_bounds();

		drawer.fit_bounds(bounds);
	}

	private void run() {

		set_text(run_button, "Stop");

		running = true;

		if (life.generation == 0) {
			life.save_rewind_state();
		}

		Thread thread = new Thread(new Runnable() {
			int n = 0;
			long start;
			long last_frame;
			int frame_time = (int) (1000 / max_fps);
			boolean interval;
			long per_frame = frame_time;

			@Override
			public void run() {
				start = new Date().getTime();
				last_frame = start - per_frame;
				interval = true;

				Runnable updater = new Runnable() {

					@Override
					public void run() {
						// System.out.println(per_frame + ">>>updater");
						// incrementCount();
						if (!running) {
							// clearInterval(interval);
							// interval.cancel();

							interval = false;

							update_hud(1000 / frame_time);
							System.out.println(frame_time + "£¡£¡>!running");

							if (onstop != null) {
								onstop.function();
							}
							return;
						}

						long time = new Date().getTime();
						// System.out.println(time + "£¡£¡>running");
						if (per_frame * n < (time - start)) {
							life.next_generation(true);
							drawer.redraw(life.root);

							n++;

							// readability ... my ass
							frame_time += (-last_frame - frame_time + (last_frame = time)) / 15;

							// System.out.println(frame_time + "£¡£¡>frame_time");
							// System.out.println(per_frame + "£¡£¡>per_frame");
							if (frame_time < .7 * per_frame) {
								n = 1;
								start = new Date().getTime();
							}
						}
						// update_hud(1000 / frame_time);

						// nextFrame(update);
						Platform.runLater(this);
					}
				};
				Platform.runLater(updater);

				while (interval) {
					// TODO should stop when exited
					try {
						Thread.sleep(666);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					System.out.println(per_frame + ">>>interval");
					// System.out.println(interval + "£¡£¡>running");
					// UI update is run on the Application thread
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							update_hud(1000 / frame_time);
						}
					});
				}
			}

		});
		// don't let thread prevent JVM shutdown
		thread.setDaemon(true);
		thread.start();
	}

	private void step(boolean is_single) {
		long time = new Date().getTime();

		if (life.generation == 0) {
			life.save_rewind_state();
		}

		life.next_generation(is_single);
		drawer.redraw(life.root);

		update_hud(1000 / (new Date().getTime() - time + 1));// TODO / by zero

		if (time < 3) {
			set_text(label_fps, "> 9000");
		}
	}

	private void update_hud() {

		set_text(label_gen, String.valueOf(life.generation));
		// set_text($("label_gen"), format_thousands(life.generation,
		// "\u202f"));

		set_text(label_pop,
				String.valueOf(life.root == null ? 0 : life.root.population));
		// format_thousands(life.root.population, "\u202f"));

		if (drawer.cell_width >= 1) {
			set_text(label_zoom, "1:" + drawer.cell_width);
		} else {
			set_text(label_zoom, 1 / drawer.cell_width + ":1");
		}
	}

	private void update_hud(double fps) {
		set_text(label_fps, String.valueOf(fps));
		update_hud();
	}

	private void lazy_redraw(TreeNode node) {
		if (!running || max_fps < 15) {
			drawer.redraw(node);// TODO bug
		}
	}

	private void set_text(Labeled obj, String text) {
		if (label_step.equals(obj)) {
			obj.setText("Step: " + text);
		} else if (label_pop.equals(obj)) {
			obj.setText("Pop: " + text);
		} else if (label_gen.equals(obj)) {
			obj.setText("Gen: " + text);
		} else if (label_fps.equals(obj)) {
			obj.setText("FPS: " + text);
		} else {
			obj.setText(text);
		}
	}

	private void do_field_move(MouseEvent e) {
		if (last_mouse) {
			double dx = Math.round(e.getSceneX() - last_mouse_x);
			double dy = Math.round(e.getSceneY() - last_mouse_y);
			System.out.println("do_field_move-dxdy: " + dx + ", " + dy);

			drawer.move(dx, dy);

			// lazy_redraw(life.root);

			last_mouse_x += dx;
			last_mouse_y += dy;
		}
	}

	private EventHandler<MouseEvent> do_field_move = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			// System.out.println(1);
			do_field_move(event);
			if (last_mouse) {
				lazy_redraw(life.root);
			}
		}
	};

	private void do_field_draw(MouseEvent e) {
		double[] coords = drawer.pixel2cell(e.getSceneX(), e.getSceneY());

		// don't draw the same pixel twice
		if (coords[0] != last_mouse_x || coords[1] != last_mouse_y) {
			life.set_bit(coords[0], coords[1], mouse_set);
			update_hud();

			drawer.draw_cell(coords[0], coords[1], mouse_set);
			last_mouse_x = coords[0];
			last_mouse_y = coords[1];
		}
	}

	private EventHandler<MouseEvent> do_field_draw = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			// System.out.println(2.3);
			do_field_draw(event);
		}
	};

}