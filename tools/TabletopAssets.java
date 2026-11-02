import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public final class TabletopAssets {
    static final int P = 16;
    static final Map<String, BufferedImage> CACHE = new HashMap<>();

    record Design(String path, double size, int footprint, String top, String leg, String base, Consumer<Graphics2D> paint, boolean rim) {}

    record Icon(String path, Consumer<Canvas> draw, String shop) {}

    static Path root;
    static Path shopDir;
    static ZipFile jar;

    public static void main(String[] args) throws Exception {
        Path pack = Path.of(args.length > 0 ? args[0] : ".");
        jar = new ZipFile(args[1]);
        root = pack.resolve("assets/brilliant");
        shopDir = pack.resolve("assets/minecraft/textures/item/custom/brilliant/shopgui");
        for (String dir : new String[]{"items/tabletop", "models/item/tabletop", "textures/item/tabletop"}) clean(root.resolve(dir));
        for (Design d : designs()) table(d);
        for (Icon icon : icons()) icon(icon);
    }

    static List<Design> designs() {
        return List.of(
                new Design("chess/table", 4.5, 4, "stripped_dark_oak_log", "dark_oak_planks", "black_wool", TabletopAssets::chess, false),
                new Design("xiangqi/table", 5.5, 5, "mangrove_planks", "stripped_mangrove_log", "black_wool", TabletopAssets::xiangqi, false),
                new Design("xiangqi/table_dark", 5.5, 5, "jungle_planks", "stripped_jungle_log", "black_wool", TabletopAssets::dark, false),
                new Design("checkers/table_2", 7.5, 7, "oak_planks", "stripped_oak_log", "brown_wool", g -> star(g, 2), false),
                new Design("checkers/table_3", 7.5, 7, "oak_planks", "stripped_oak_log", "brown_wool", g -> star(g, 3), false),
                new Design("checkers/table_4", 7.5, 7, "oak_planks", "stripped_oak_log", "brown_wool", g -> star(g, 4), false),
                new Design("bno/table", 5.5, 5, "black_concrete", "gray_concrete", "black_wool", TabletopAssets::uno, false),
                new Design("poker/table_texas_holdem", 5.5, 5, "dark_oak_planks", "stripped_dark_oak_log", "black_wool", TabletopAssets::holdem, false),
                new Design("poker/table_blackjack", 5.5, 5, "spruce_planks", "stripped_spruce_log", "black_wool", TabletopAssets::blackjack, false),
                new Design("poker/table_bridge", 5.5, 5, "spruce_planks", "stripped_spruce_log", "brown_wool", TabletopAssets::bridge, false),
                new Design("poker/table_big_two", 5.5, 5, "crimson_planks", "stripped_mangrove_log", "black_wool", TabletopAssets::bigTwo, false),
                new Design("poker/table_old_maid", 5.5, 5, "dark_oak_planks", "stripped_dark_oak_log", "black_wool", TabletopAssets::oldMaid, false),
                new Design("poker/table_pick_red_dots", 5.5, 5, "oak_planks", "stripped_oak_log", "brown_wool", TabletopAssets::redDots, false),
                new Design("poker/table_solitaire", 5.5, 5, "jungle_planks", "stripped_jungle_log", "brown_wool", TabletopAssets::solitaire, false),
                new Design("mahjong/table_riichi", 3.5, 3, "gray_concrete", "polished_blackstone", "black_concrete", g -> mahjong(g, "green_wool", false), true),
                new Design("mahjong/table_sanma", 3.5, 3, "gray_concrete", "polished_blackstone", "black_concrete", g -> mahjong(g, "blue_wool", false), true),
                new Design("mahjong/table_taiwan", 3.5, 3, "dark_oak_planks", "stripped_dark_oak_log", "black_wool", g -> mahjong(g, "red_wool", true), true)
        );
    }

    static List<Icon> icons() {
        List<Icon> list = new ArrayList<>();
        add(list, "common/plate_empty", c -> plate(c, GRAY_R, false));
        add(list, "common/plate_seated", c -> plate(c, CREAM, false));
        add(list, "common/plate_ready", c -> plate(c, LIME, false));
        add(list, "common/plate_turn", c -> plate(c, ORANGE, true));
        add(list, "common/plate_bot", TabletopAssets::botPlate);
        list.add(new Icon("common/ready", null, "confirm1"));
        add(list, "common/key_pass", c -> key(c, GRAY_R, "過"));
        add(list, "common/start", c -> c.shape(PLAY, 4, 4, LIME));
        add(list, "chess/sign", c -> { sign(c, WOOD_R); c.shape(QUEEN, 3, 3, WHITE_R); });
        add(list, "chess/move", c -> c.flat(circle(6), 5, 5, alpha(INK_R.deep, 120)));
        add(list, "chess/capture", c -> corners(c, PINK));
        add(list, "chess/last", c -> { for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) c.set(x, y, alpha(YELLOW_R.base, x == 0 || y == 0 || x == 15 || y == 15 ? 190 : 90)); });
        add(list, "chess/check", c -> glow(c, RED_R));
        add(list, "chess/promote_queen", c -> { keyCap(c, 0, 0, 16, 14, WOOD_R); c.shape(QUEEN, 3, 2, WHITE_R); });
        add(list, "chess/promote_rook", c -> { keyCap(c, 0, 0, 16, 14, WOOD_R); c.shape(ROOK, 3, 2, WHITE_R); });
        add(list, "chess/promote_bishop", c -> { keyCap(c, 0, 0, 16, 14, WOOD_R); c.shape(BISHOP, 3, 2, WHITE_R); });
        add(list, "chess/promote_knight", c -> { keyCap(c, 0, 0, 16, 14, WOOD_R); c.shape(KNIGHT, 3, 2, WHITE_R); });
        add(list, "chess/clock_white", c -> clock(c, true));
        add(list, "chess/clock_black", c -> clock(c, false));

        add(list, "xiangqi/sign", c -> { sign(c, WOOD_R); c.shape(circle(12), 2, 2, CREAM); c.shape(ring(12, 1), 2, 2, RED_R); boolean[][] h = hanzi("帥"); c.glyph(h, cx(h), cy(h), RED_R.base, RED_R.deep); });
        add(list, "xiangqi/sign_dark", c -> { sign(c, WOOD_R); c.shape(circle(12), 2, 2, JADE); });
        add(list, "xiangqi/move", c -> c.flat(circle(6), 5, 5, alpha(GREEN_R.deep, 170)));
        add(list, "xiangqi/capture", c -> c.shape(ring(16, 3), 0, 0, PINK));
        add(list, "xiangqi/last", c -> c.shape(ring(16, 2), 0, 0, GOLD_R));
        add(list, "xiangqi/check", c -> { glow(c, RED_R); boolean[][] h = hanzi("將"); c.glyph(h, cx(h), cy(h), GLYPH, RED_R.deep); });

        add(list, "checkers/sign", c -> { sign(c, WOOD_R); c.shape(STAR, 1, 2, CREAM); c.set(7, 4, RED_R.base); c.set(4, 7, BLUE_R.base); c.set(10, 7, GREEN_R.base); });
        add(list, "checkers/move", c -> c.shape(circle(8), 4, 4, LIME));
        add(list, "checkers/path", c -> c.shape(circle(6), 5, 5, ORANGE));
        add(list, "checkers/landing", c -> { c.shape(ring(16, 3), 0, 0, GOLD_R); c.shape(circle(4), 6, 6, GOLD_R); });

        add(list, "bno/sign", c -> { sign(c, INK_R); card(c, 2, 3, 6, 9, RED_R); card(c, 5, 4, 6, 9, YELLOW_R); card(c, 8, 5, 6, 9, BLUE_R); });
        add(list, "bno/color_red", c -> c.shape(frameMask(16, 16, 4), 0, 0, RED_R));
        add(list, "bno/color_yellow", c -> c.shape(frameMask(16, 16, 4), 0, 0, YELLOW_R));
        add(list, "bno/color_green", c -> c.shape(frameMask(16, 16, 4), 0, 0, GREEN_R));
        add(list, "bno/color_blue", c -> c.shape(frameMask(16, 16, 4), 0, 0, BLUE_R));
        list.add(new Icon("bno/cw", null, "reload"));
        add(list, "bno/ccw", c -> c.image(flipImage(shop("reload")), 0, 0));
        add(list, "bno/uno", c -> { c.shape(burst(), 0, 0, YELLOW_R); plaque(c, 0, 4, 16, 9, RED_R); c.glyph(text("BNO!"), 0, 6, GLYPH, RED_R.deep); });
        add(list, "bno/penalty_2", c -> penalty(c, 2, RED_R));
        add(list, "bno/penalty_4", c -> penalty(c, 4, INK_R));

        add(list, "poker/sign", c -> { sign(c, GREEN_R); card(c, 2, 2, 7, 11, WHITE_R); card(c, 7, 3, 7, 11, WHITE_R); c.set(4, 4, INK_R.base); c.set(9, 5, RED_R.base); });
        add(list, "poker/dealer", c -> disc(c, WHITE_R, "D"));
        add(list, "poker/small_blind", c -> disc(c, BLUE_R, "SB"));
        add(list, "poker/big_blind", c -> disc(c, GOLD_R, "BB"));
        add(list, "poker/bet", c -> stack(c, 5, 12, 4, RED_R));
        add(list, "poker/pot", c -> { stack(c, 0, 13, 3, RED_R); stack(c, 5, 13, 5, BLUE_R); stack(c, 10, 13, 2, INK_R); });
        add(list, "poker/fold", c -> { backCard(c, 1, 2); backCard(c, 6, 3); c.shape(CROSS, 3, 4, PINK); });
        add(list, "poker/all_in", c -> { stack(c, 1, 13, 4, RED_R); stack(c, 9, 13, 4, BLUE_R); plaque(c, 0, 0, 16, 7, RED_R); c.glyph(text("ALL"), 3, 1, GLYPH, RED_R.deep); });
        add(list, "poker/bj_bet", c -> { c.shape(ring(16, 2), 0, 0, GOLD_R); stack(c, 5, 10, 3, RED_R); });
        list.add(new Icon("poker/bust", null, "close"));
        add(list, "poker/blackjack", c -> c.shape(STAR, 1, 2, GOLD_R));
        add(list, "poker/split", c -> { card(c, 0, 2, 7, 12, WHITE_R); card(c, 9, 2, 7, 12, WHITE_R); });
        add(list, "poker/double", c -> { stack(c, 1, 13, 3, RED_R); stack(c, 9, 13, 3, RED_R); c.glyph(sprite("#.#", ".#.", "#.#"), 3, 2, GLYPH, GLYPH_SHADE); c.glyph(digit(2), 8, 0, GLYPH, GLYPH_SHADE); });
        add(list, "poker/trump_clubs", c -> trump(c, CLUB, INK_R));
        add(list, "poker/trump_diamonds", c -> trump(c, DIAMOND_M, RED_R));
        add(list, "poker/trump_hearts", c -> trump(c, HEART_M, RED_R));
        add(list, "poker/trump_spades", c -> trump(c, SPADE, INK_R));
        add(list, "poker/trump_nt", c -> { card(c, 2, 0, 12, 16, CREAM); c.glyph(text("NT"), 4, 6, INK_R.base, null); });
        add(list, "poker/declarer", c -> disc(c, GOLD_R, "D"));
        add(list, "poker/dummy", c -> { card(c, 0, 2, 7, 12, WHITE_R); card(c, 4, 2, 7, 12, WHITE_R); card(c, 9, 2, 7, 12, WHITE_R); });
        add(list, "poker/doubled", c -> { plaque(c, 1, 2, 14, 12, RED_R); c.glyph(text("X"), 7, 6, GLYPH, RED_R.deep); });
        add(list, "poker/redoubled", c -> { plaque(c, 1, 2, 14, 12, BLUE_R); c.glyph(text("XX"), 5, 6, GLYPH, BLUE_R.deep); });
        add(list, "poker/lead", c -> { keyCap(c, 0, 0, 16, 14, GOLD_R); c.glyph(PLAY, 4, 3, GLYPH, GOLD_R.deep); });
        add(list, "poker/draw_from", c -> { backCard(c, 0, 2); backCard(c, 3, 3); c.shape(ARROW, 8, 5, ORANGE); });

        add(list, "mahjong/sign", c -> { sign(c, JADE); c.shape(rect(10, 13, true), 3, 1, WHITE_R); c.flat(sprite("...#...", "#######", "#..#..#", "#######", "...#...", "...#..."), 4, 4, RED_R.base); });
        String[][] keys = {{"chii", "吃"}, {"pon", "碰"}, {"kan", "槓"}, {"ron", "榮"}, {"tsumo", "摸"}, {"riichi", "立"}, {"kita", "北"}, {"kyuushu", "流"}};
        Ramp[] keyColors = {LIME, TEAL, PURPLE, PINK, ORANGE, HOT, BLUE_R, INK_R};
        for (int i = 0; i < keys.length; i++) {
            String h = keys[i][1];
            Ramp r = keyColors[i];
            add(list, "mahjong/key_" + keys[i][0], c -> key(c, r, h));
        }
        add(list, "mahjong/riichi_stick", c -> rod(c));
        add(list, "mahjong/dealer", c -> { plaque(c, 0, 1, 16, 14, RED_R); boolean[][] h = hanzi("莊"); c.glyph(h, cx(h), 1 + (14 - h.length) / 2, GLYPH, RED_R.deep); });
        add(list, "mahjong/wind_on", c -> wind(c));
        return list;
    }

    static void add(List<Icon> list, String path, Consumer<Canvas> draw) { list.add(new Icon(path, draw, null)); }

    static void clean(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    static void png(String path, BufferedImage img) throws IOException {
        Path file = root.resolve("textures/item/tabletop/" + path + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(img, "png", file.toFile());
    }

    static String ref(String path) { return "brilliant:item/tabletop/" + path; }

    static void item(String path) throws IOException {
        write(root.resolve("items/tabletop/" + path + ".json"),
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + ref(path) + "\"}}\n");
    }

    static void icon(Icon icon) throws IOException {
        String texture;
        if (icon.shop != null) texture = "minecraft:item/custom/brilliant/shopgui/" + icon.shop;
        else {
            Canvas c = new Canvas();
            icon.draw.accept(c);
            png(icon.path, c.image());
            texture = ref(icon.path);
        }
        String textures = "\"textures\":{\"0\":\"" + texture + "\",\"particle\":\"" + texture + "\"}";
        String up = "{\"uv\":[0,0,16,16],\"texture\":\"#0\"}";
        write(root.resolve("models/item/tabletop/" + icon.path + ".json"),
                "{" + textures + ",\"elements\":[{\"from\":[0,8,0],\"to\":[16,8,16],\"shade\":false,\"faces\":{\"up\":" + up + ",\"down\":{\"uv\":[0,16,16,0],\"texture\":\"#0\"}}}]}\n");
        write(root.resolve("models/item/tabletop/" + icon.path + "_stand.json"),
                "{" + textures + ",\"elements\":[{\"from\":[0,0,8],\"to\":[16,16,8],\"shade\":false,\"faces\":{\"north\":{\"uv\":[16,0,0,16],\"texture\":\"#0\"},\"south\":" + up + "}}]}\n");
        item(icon.path);
        item(icon.path + "_stand");
    }

    static BufferedImage tiled(String material, int w, int h) {
        BufferedImage t = tex(material), img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) img.setRGB(x, y, t.getRGB(x % 16, y % 16));
        return img;
    }

    static int pxOf(double blocks) { return (int) Math.round(blocks * P); }

    static String num(double v) {
        String s = String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    static String cube(double[] from, double[] to, String side, String cap, String top) {
        String f = "[" + num(from[0]) + "," + num(from[1]) + "," + num(from[2]) + "]";
        String t = "[" + num(to[0]) + "," + num(to[1]) + "," + num(to[2]) + "]";
        String s = "{\"uv\":[0,0,16,16],\"texture\":\"#" + side + "\"}";
        String c = "{\"uv\":[0,0,16,16],\"texture\":\"#" + cap + "\"}";
        String u = "{\"uv\":[0,0,16,16],\"texture\":\"#" + top + "\"}";
        return "{\"from\":" + f + ",\"to\":" + t + ",\"faces\":{\"north\":" + s + ",\"south\":" + s + ",\"east\":" + s + ",\"west\":" + s + ",\"up\":" + u + ",\"down\":" + c + "}}";
    }

    static void table(Design d) throws IOException {
        double S = d.size, k = 16 / S, half = S / 2, fp = d.footprint / 2.0;
        double topY = 1.0625, thick = 0.1875, apron = 0.125, inset = fp - 0.3125, leg = 0.25, li = half - 0.1875, rim = 0.1875, rimH = 0.125;
        png(d.path + "_top", topTexture(d));
        StringBuilder textures = new StringBuilder("\"top\":\"" + ref(d.path + "_top") + "\"");
        Map<String, String[]> parts = new LinkedHashMap<>();
        parts.put("slab", new String[]{d.top, "" + pxOf(S), "" + pxOf(thick)});
        parts.put("slab_cap", new String[]{d.top, "" + pxOf(S), "" + pxOf(S)});
        parts.put("apron", new String[]{d.leg, "" + pxOf(S - 0.125), "" + pxOf(apron)});
        parts.put("apron_cap", new String[]{d.leg, "" + pxOf(S - 0.125), "" + pxOf(S - 0.125)});
        parts.put("base", new String[]{d.base, "" + pxOf(inset * 2), "" + pxOf(topY - thick - apron)});
        parts.put("base_cap", new String[]{d.base, "" + pxOf(inset * 2), "" + pxOf(inset * 2)});
        parts.put("leg", new String[]{d.leg, "" + pxOf(leg), "" + pxOf(topY - thick)});
        parts.put("leg_cap", new String[]{d.leg, "" + pxOf(leg), "" + pxOf(leg)});
        if (d.rim) {
            parts.put("rim_long", new String[]{d.top, "" + pxOf(S), "" + pxOf(rimH)});
            parts.put("rim_top", new String[]{d.top, "" + pxOf(S), "" + pxOf(rim)});
        }
        for (var e : parts.entrySet()) {
            String[] v = e.getValue();
            String name = "common/material/" + v[0] + "_" + v[1] + "x" + v[2];
            png(name, tiled(v[0], Integer.parseInt(v[1]), Integer.parseInt(v[2])));
            textures.append(",\"").append(e.getKey()).append("\":\"").append(ref(name)).append("\"");
        }
        textures.append(",\"particle\":\"minecraft:block/").append(d.top).append("\"");
        List<String> el = new ArrayList<>();
        el.add(cube(new double[]{0, (topY - thick) * k, 0}, new double[]{16, topY * k, 16}, "slab", "slab_cap", "top"));
        double a = 0.0625 * k;
        el.add(cube(new double[]{a, (topY - thick - apron) * k, a}, new double[]{16 - a, (topY - thick) * k, 16 - a}, "apron", "apron_cap", "apron_cap"));
        double b = (half - inset) * k;
        el.add(cube(new double[]{b, 0, b}, new double[]{16 - b, (topY - thick - apron) * k, 16 - b}, "base", "base_cap", "base_cap"));
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1}) {
                double ux = 8 + sx * li * k, uz = 8 + sz * li * k, h = leg / 2 * k;
                el.add(cube(new double[]{ux - h, 0, uz - h}, new double[]{ux + h, (topY - thick) * k, uz + h}, "leg", "leg_cap", "leg_cap"));
            }
        if (d.rim) {
            double r = rim * k, y0 = topY * k, y1 = (topY + rimH) * k;
            el.add(cube(new double[]{0, y0, 0}, new double[]{16, y1, r}, "rim_long", "rim_top", "rim_top"));
            el.add(cube(new double[]{0, y0, 16 - r}, new double[]{16, y1, 16}, "rim_long", "rim_top", "rim_top"));
            el.add(cube(new double[]{0, y0, r}, new double[]{r, y1, 16 - r}, "rim_long", "rim_top", "rim_top"));
            el.add(cube(new double[]{16 - r, y0, r}, new double[]{16, y1, 16 - r}, "rim_long", "rim_top", "rim_top"));
        }
        write(root.resolve("models/item/tabletop/" + d.path + ".json"),
                "{\"textures\":{" + textures + "},\"elements\":[" + String.join(",", el) + "]}\n");
        item(d.path);
    }

    static BufferedImage tex(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            try (var in = jar.getInputStream(jar.getEntry("assets/minecraft/textures/block/" + n + ".png"))) {
                BufferedImage src = ImageIO.read(in);
                BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                img.getGraphics().drawImage(src, 0, 0, null);
                return img;
            } catch (Exception e) {
                throw new RuntimeException(n, e);
            }
        });
    }

    static BufferedImage shop(String name) {
        try {
            BufferedImage src = ImageIO.read(shopDir.resolve(name + ".png").toFile());
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.getGraphics().drawImage(src, 0, 0, null);
            return img;
        } catch (Exception e) {
            throw new RuntimeException(name, e);
        }
    }

    static BufferedImage flipImage(BufferedImage src) {
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) out.setRGB(15 - x, y, src.getRGB(x, y));
        return out;
    }

    static void crisp(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    static void fill(Graphics2D g, Shape s, String texture) {
        BufferedImage t = tex(texture);
        g.setPaint(new TexturePaint(t, new Rectangle2D.Double(0, 0, 1, 1)));
        g.fill(s);
    }

    static BufferedImage painted(Color c, long seed) {
        BufferedImage t = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        Color[] pal = {shade(c, 0.92), c, shade(c, 1.06)};
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int n = r.nextInt(100);
                t.setRGB(x, y, (n < 18 ? pal[0] : n > 88 ? pal[2] : pal[1]).getRGB());
            }
        return t;
    }

    static void paint(Graphics2D g, Shape s, Color c, long seed) {
        g.setPaint(new TexturePaint(painted(c, seed), new Rectangle2D.Double(0, 0, 1, 1)));
        g.fill(s);
    }

    static void fill(Graphics2D g, Shape s, Color c) {
        g.setColor(c);
        g.fill(s);
    }

    static double px(double v) { return v / P; }

    static Rectangle2D rect(double cx, double cy, double w, double h) { return new Rectangle2D.Double(cx - w / 2, cy - h / 2, w, h); }

    static Rectangle2D box(double x0, double y0, double x1, double y1) { return new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0); }

    static void frame(Graphics2D g, Rectangle2D r, Color c) {
        double t = px(1);
        fill(g, box(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMinY() + t), c);
        fill(g, box(r.getMinX(), r.getMaxY() - t, r.getMaxX(), r.getMaxY()), c);
        fill(g, box(r.getMinX(), r.getMinY(), r.getMinX() + t, r.getMaxY()), c);
        fill(g, box(r.getMaxX() - t, r.getMinY(), r.getMaxX(), r.getMaxY()), c);
    }

    static void dotted(Graphics2D g, Rectangle2D r, Color c) {
        double t = px(1);
        for (double x = r.getMinX(); x < r.getMaxX() - 1e-6; x += 2 * t) {
            fill(g, box(x, r.getMinY(), x + t, r.getMinY() + t), c);
            fill(g, box(x, r.getMaxY() - t, x + t, r.getMaxY()), c);
        }
        for (double y = r.getMinY(); y < r.getMaxY() - 1e-6; y += 2 * t) {
            fill(g, box(r.getMinX(), y, r.getMinX() + t, y + t), c);
            fill(g, box(r.getMaxX() - t, y, r.getMaxX(), y + t), c);
        }
    }

    static void pixel(Graphics2D g, double x, double y, Color c) { fill(g, new Rectangle2D.Double(x, y, px(1), px(1)), c); }

    static void ring(Graphics2D g, double cx, double cy, double r, Color c) {
        int n = (int) Math.round(r * P);
        for (int y = -n - 1; y <= n; y++)
            for (int x = -n - 1; x <= n; x++) {
                double d = Math.hypot(x + 0.5, y + 0.5);
                if (d <= n + 0.5 && d > n - 0.5) pixel(g, cx + px(x), cy + px(y), c);
            }
    }

    static void disc(Graphics2D g, double cx, double cy, double r, Color c) {
        int n = (int) Math.round(r * P);
        for (int y = -n - 1; y <= n; y++)
            for (int x = -n - 1; x <= n; x++)
                if (Math.hypot(x + 0.5, y + 0.5) <= n) pixel(g, cx + px(x), cy + px(y), c);
    }

    static Shape oval(double w, double h) { return new RoundRectangle2D.Double(-w / 2, -h / 2, w, h, Math.min(w, h), Math.min(w, h)); }

    static final Map<Character, String> FONT = new HashMap<>();

    static {
        String[] glyphs = {
                "0", "111101101101111", "1", "010110010010111", "2", "111001111100111", "3", "111001111001111",
                "4", "101101111001001", "5", "111100111001111", "6", "111100111101111", "7", "111001001001001",
                "8", "111101111101111", "9", "111101111001111", "A", "010101111101101", "B", "110101110101110",
                "C", "011100100100011", "D", "110101101101110", "E", "111100110100111", "J", "001001001101010",
                "K", "101101110101101", "L", "100100100100111", "N", "101111111101101", "O", "010101101101010",
                "P", "110101110100100", "S", "011100010001110", "T", "111010010010010", "W", "101101111111101",
                "!", "010010010000010", ":", "000010000010000", "+", "000010111010000", "X", "101101010101101"};
        for (int i = 0; i < glyphs.length; i += 2) FONT.put(glyphs[i].charAt(0), glyphs[i + 1]);
    }

    static void pixelText(Graphics2D g, String s, double cx, double cy, Color c) {
        double w = px(s.length() * 4 - 1), x0 = cx - Math.floor(w * P / 2) / P, y0 = cy - Math.floor(2.5) / P;
        for (int i = 0; i < s.length(); i++) {
            String bits = FONT.get(s.charAt(i));
            if (bits == null) continue;
            for (int k = 0; k < 15; k++) if (bits.charAt(k) == '1') pixel(g, x0 + px(i * 4 + k % 3), y0 + px(k / 3), c);
        }
    }

    static BufferedImage topTexture(Design d) {
        int n = (int) Math.round(d.size * P);
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        crisp(g);
        g.scale(P, P);
        g.translate(d.size / 2, d.size / 2);
        fill(g, rect(0, 0, d.size, d.size), d.top);
        d.paint.accept(g);
        g.dispose();
        return img;
    }

    static final Color GOLD = new Color(0xE0B84A);
    static final Color INK = new Color(0x3A2410);

    static void chess(Graphics2D g) {
        fill(g, rect(0, 0, 4.0 + px(2), 4.0 + px(2)), GOLD);
        for (int f = 0; f < 8; f++)
            for (int r = 0; r < 8; r++)
                paint(g, new Rectangle2D.Double(-2 + f * 0.5, -2 + r * 0.5, 0.5, 0.5), (f + r) % 2 == 0 ? new Color(0xD8C49A) : new Color(0x6B4A2E), (f + r) % 2);
    }

    static void xiangqi(Graphics2D g) {
        paint(g, rect(0, 0, 4.75, 5.25), new Color(0xD9BF86), 3);
        frame(g, rect(0, 0, 4.75, 5.25), new Color(0x5E1A10));
        double s = 0.5, w = 4, d = 4.5, t = px(1);
        frame(g, rect(0, 0, w + px(5), d + px(5)), INK);
        for (int r = 0; r <= 9; r++) fill(g, box(-w / 2, -d / 2 + r * s, w / 2 + t, -d / 2 + r * s + t), INK);
        for (int f = 0; f <= 8; f++) {
            double x = -w / 2 + f * s;
            if (f == 0 || f == 8) fill(g, box(x, -d / 2, x + t, d / 2 + t), INK);
            else {
                fill(g, box(x, -d / 2, x + t, -0.25), INK);
                fill(g, box(x, 0.25, x + t, d / 2 + t), INK);
            }
        }
        for (int side : new int[]{-1, 1})
            for (int k = 0; k <= 16; k++) {
                double y0 = side < 0 ? -d / 2 : d / 2 - 1;
                pixel(g, -0.5 + px(k), y0 + px(k), INK);
                pixel(g, 0.5 - px(k), y0 + px(k), INK);
            }
        int[][] marks = {{1, 2}, {7, 2}, {0, 3}, {2, 3}, {4, 3}, {6, 3}, {8, 3}, {1, 7}, {7, 7}, {0, 6}, {2, 6}, {4, 6}, {6, 6}, {8, 6}};
        for (int[] m : marks) {
            double x = -w / 2 + m[0] * s, y = -d / 2 + m[1] * s;
            for (int sx : new int[]{-1, 1}) {
                if ((m[0] == 0 && sx < 0) || (m[0] == 8 && sx > 0)) continue;
                for (int sy : new int[]{-1, 1}) {
                    pixel(g, x + px(sx * 2), y + px(sy * 2), INK);
                    pixel(g, x + px(sx * 3), y + px(sy * 2), INK);
                    pixel(g, x + px(sx * 2), y + px(sy * 3), INK);
                }
            }
        }
        Color water = new Color(0x4E7FA8);
        for (int k = 0; k < 64; k += 4) {
            double x = -2 + px(k + 2);
            pixel(g, x, -px(2), water);
            pixel(g, x + px(1), -px(3), water);
            pixel(g, x + px(2), -px(2), water);
            pixel(g, x + px(1), px(2), water);
            pixel(g, x + px(2), px(1), water);
            pixel(g, x + px(3), px(2), water);
        }
    }

    static void dark(Graphics2D g) {
        paint(g, rect(0, 0, 4.75, 4.75), new Color(0xD9BF86), 3);
        frame(g, rect(0, 0, 4.75, 4.75), new Color(0x4A2010));
        for (int c = 0; c < 8; c++)
            for (int r = 0; r < 4; r++)
                if ((c + r) % 2 == 1) paint(g, new Rectangle2D.Double(-2 + c * 0.5, -1 + r * 0.5, 0.5, 0.5), new Color(0xC4A672), 4);
        double t = px(1);
        for (int c = 0; c <= 8; c++) fill(g, box(-2 + c * 0.5, -1, -2 + c * 0.5 + t, 1 + t), INK);
        for (int r = 0; r <= 4; r++) fill(g, box(-2, -1 + r * 0.5, 2 + t, -1 + r * 0.5 + t), INK);
        for (int i = 0; i < 16; i++) {
            ring(g, -1.75 + (i % 8) * 0.5, -1.5 - (i / 8) * 0.375, px(2), new Color(0x8A2416));
            ring(g, -1.75 + (i % 8) * 0.5, 1.5 + (i / 8) * 0.375, px(2), INK);
        }
    }

    static final String[] SEAT_WOOL = {"red_wool", "blue_wool", "green_wool", "yellow_wool"};

    static void star(Graphics2D g, int players) {
        int[] widths = {1, 2, 3, 4, 13, 12, 11, 10, 9, 10, 11, 12, 13, 4, 3, 2, 1};
        int[] used = Map.of(2, new int[]{0, 3}, 3, new int[]{0, 2, 4}, 4, new int[]{0, 1, 3, 4}).get(players);
        double step = 0.4, rowStep = step * Math.sqrt(3) / 2;
        Path2D hex = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(30 + i * 60), r = 3.6;
            if (i == 0) hex.moveTo(Math.cos(a) * r, Math.sin(a) * r); else hex.lineTo(Math.cos(a) * r, Math.sin(a) * r);
        }
        hex.closePath();
        fill(g, hex, "spruce_planks");
        double tip = 8 * rowStep + 0.3, mid = 4 * rowStep;
        for (int c = 0; c < 6; c++) {
            double base = Math.toRadians(-90 + c * 60), hw = 2 * step + 0.3;
            double bx = Math.cos(base) * mid, by = Math.sin(base) * mid;
            Path2D tri = new Path2D.Double();
            tri.moveTo(Math.cos(base) * tip, Math.sin(base) * tip);
            tri.lineTo(bx + Math.cos(base + Math.PI / 2) * hw, by + Math.sin(base + Math.PI / 2) * hw);
            tri.lineTo(bx - Math.cos(base + Math.PI / 2) * hw, by - Math.sin(base + Math.PI / 2) * hw);
            tri.closePath();
            int seat = -1;
            for (int k = 0; k < used.length; k++) if (used[k] == c) seat = k;
            if (seat >= 0) fill(g, tri, SEAT_WOOL[seat]); else paint(g, tri, new Color(0xE2D2AE), 5);
        }
        Path2D hexIn = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60), r = 4 * step + 0.32;
            if (i == 0) hexIn.moveTo(Math.cos(a) * r, Math.sin(a) * r); else hexIn.lineTo(Math.cos(a) * r, Math.sin(a) * r);
        }
        hexIn.closePath();
        paint(g, hexIn, new Color(0xE2D2AE), 5);
        for (int row = 0; row < 17; row++)
            for (int i = 0; i < widths[row]; i++) {
                int x = -(widths[row] - 1) + i * 2;
                double hx = Math.floor(x * step / 2 * P) / P, hz = Math.floor((row - 8) * rowStep * P) / P;
                fill(g, new Rectangle2D.Double(hx - px(1), hz - px(1), px(2), px(2)), new Color(0x3A2814));
            }
    }

    static void uno(Graphics2D g) {
        String[] cs = {"red_concrete", "yellow_concrete", "green_concrete", "blue_concrete"};
        for (int i = 0; i < 4; i++) fill(g, new Arc2D.Double(-2.5, -2.5, 5.0, 5.0, i * 90 + 8, 74, Arc2D.PIE), cs[i]);
        fill(g, new Ellipse2D.Double(-2.125, -2.125, 4.25, 4.25), "black_concrete");
        fill(g, new Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0), "black_wool");
        Color mark = new Color(0x9A9AA2);
        dotted(g, rect(-0.375, 0, 0.625, 0.625), mark);
        dotted(g, rect(0.375, 0, 0.625, 0.625), mark);
    }

    static void rail(Graphics2D g, Shape felt, String wool, String railWool) {
        Rectangle2D b = felt.getBounds2D();
        fill(g, new RoundRectangle2D.Double(b.getX() - 0.3125, b.getY() - 0.3125, b.getWidth() + 0.625, b.getHeight() + 0.625, 2.75, 2.75), railWool);
        fill(g, felt, wool);
    }

    static void holdem(Graphics2D g) {
        rail(g, oval(4.375, 3.375), "green_wool", "black_wool");
        for (int i = 0; i < 5; i++) dotted(g, rect(-1.0 + i * 0.5, 0, 0.375, 0.5), GOLD);
        ring(g, 0, 0.75, 0.25, GOLD);
    }

    static void blackjack(Graphics2D g) {
        rail(g, oval(4.375, 3.5), "cyan_wool", "brown_wool");
        pixelText(g, "BLACKJACK 3:2", 0, 0.5, GOLD);
        fill(g, box(-1.5, 0.75, 1.5, 0.75 + px(1)), GOLD);
        for (int s = 0; s < 4; s++) {
            double a = Math.toRadians(s * 90);
            ring(g, Math.round(Math.cos(a) * 1.375 * 8) / 8.0, Math.round(Math.sin(a) * 1.125 * 8) / 8.0, px(3), GOLD);
        }
        for (int i = 0; i < 4; i++) dotted(g, rect(-0.5625 + i * 0.375, -0.375, 0.3125, 0.4375), new Color(0xB8D8D0));
    }

    static void bridge(Graphics2D g) {
        Rectangle2D baize = rect(0, 0, 4.75, 4.75);
        fill(g, baize, "green_wool");
        frame(g, baize, GOLD);
        ring(g, 0, 0, 0.75, GOLD);
        pixelText(g, "N", 0, -1.0, GOLD);
        pixelText(g, "E", 1.0, 0, GOLD);
        pixelText(g, "S", 0, 1.0, GOLD);
        pixelText(g, "W", -1.0, 0, GOLD);
        for (int i = 0; i < 4; i++) dotted(g, rect(i == 0 || i == 3 ? -2.0 : 2.0, i < 2 ? -2.0 : 2.0, 0.875, 0.875), new Color(0x9CC89C));
    }

    static void bigTwo(Graphics2D g) {
        Rectangle2D cloth = rect(0, 0, 4.75, 4.75);
        fill(g, cloth, "red_wool");
        frame(g, rect(0, 0, 4.5, 4.5), GOLD);
        frame(g, rect(0, 0, 4.5 - px(4), 4.5 - px(4)), GOLD);
        Rectangle2D area = rect(0, 0, 2.5, 1.0);
        fill(g, area, "red_concrete");
        frame(g, area, GOLD);
        for (int i = 0; i < 4; i++) {
            double x = i % 2 == 0 ? -1.875 : 1.875, y = i < 2 ? -1.875 : 1.875;
            ring(g, x, y, px(2), GOLD);
            pixel(g, x, y, GOLD);
        }
    }

    static void oldMaid(Graphics2D g) {
        rail(g, oval(4.375, 3.75), "purple_wool", "black_wool");
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 + i * 90);
            for (int k = -3; k <= 3; k++) {
                double b = a + k * 0.09;
                pixel(g, Math.cos(b) * 1.4, Math.sin(b) * 1.15, new Color(0xE0CDEB));
            }
        }
        Rectangle2D card = rect(0, 0, 0.5, 0.75);
        fill(g, card, "white_concrete");
        frame(g, card, new Color(0x2A1C3A));
        pixelText(g, "JK", 0, 0, new Color(0x7A2A9A));
    }

    static final String[] HEART = {"##.##", "#####", "#####", ".###.", "..#.."};
    static final String[] DIAMOND = {"..#..", ".###.", "#####", ".###.", "..#.."};

    static void pip(Graphics2D g, String[] rows, double cx, double cy, Color c) {
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++)
                if (rows[j].charAt(i) == '#') pixel(g, cx + px(i - 2), cy + px(j - 2), c);
    }

    static void redDots(Graphics2D g) {
        Rectangle2D mat = rect(0, 0, 4.0, 1.75);
        fill(g, mat, "green_wool");
        frame(g, mat, new Color(0x2E4A1C));
        Color line = new Color(0xC8E0B0);
        for (int side : new int[]{-1, 1})
            for (int k = 0; k < 3; k++)
                for (int row : new int[]{-1, 1}) {
                    Rectangle2D slot = rect(side * (0.5625 + k * 0.5), row * 0.375, 0.375, 0.5625);
                    paint(g, slot, new Color(0x4E7A2A), 8);
                    frame(g, slot, line);
                }
        Rectangle2D stock = rect(0, 0, 0.4375, 0.625);
        paint(g, stock, new Color(0x2A4A8A), 6);
        frame(g, stock, new Color(0xE6E0D0));
        frame(g, rect(0, 0, 0.4375 - px(4), 0.625 - px(4)), new Color(0x8AA0D0));
        Color red = new Color(0xC02A22);
        for (int i = 0; i < 4; i++) {
            double x = i == 0 || i == 3 ? -2.0 : 2.0, y = i < 2 ? -2.0 : 2.0;
            Rectangle2D pile = rect(x, y, 0.5, 0.875);
            paint(g, pile, new Color(0xE8DCC0), 7);
            frame(g, pile, new Color(0x6A4A2A));
            pip(g, HEART, x, y - px(3), red);
            pip(g, DIAMOND, x, y + px(3), red);
        }
    }

    static void solitaire(Graphics2D g) {
        Rectangle2D cloth = rect(0, 0, 5.0, 5.0);
        fill(g, cloth, "green_wool");
        frame(g, rect(0, 0, 4.875, 4.875), GOLD);
        Color line = new Color(0xB8E0B0);
        dotted(g, rect(-1.875, -1.875, 0.5, 0.375), line);
        dotted(g, rect(-1.875, -1.25, 0.5, 0.375), line);
        Color[] suit = {new Color(0x1A1A1A), new Color(0xB02E26), new Color(0xB02E26), new Color(0x1A1A1A)};
        for (int i = 0; i < 4; i++) {
            dotted(g, rect(-1.875, -0.3125 + i * 0.625, 0.5, 0.375), line);
            disc(g, -1.875, -0.3125 + i * 0.625, px(1), suit[i]);
        }
        for (int c = 0; c < 7; c++) {
            double z = -1.875 + c * 0.625;
            dotted(g, rect(-1.25, z, 0.5, 0.375), line);
            pixelText(g, String.valueOf(c + 1), -1.25, z, line);
        }
    }

    static void mahjong(Graphics2D g, String wool, boolean taiwan) {
        Rectangle2D felt = rect(0, 0, 3.0, 3.0);
        fill(g, felt, wool);
        if (taiwan) frame(g, rect(0, 0, 2.875, 2.875), GOLD);
        Rectangle2D panel = rect(0, 0, 0.75, 0.75);
        fill(g, panel, "black_concrete");
        frame(g, panel, new Color(0x6A7076));
        Color off = new Color(0x5A6066);
        fill(g, rect(0, -0.25, px(4), px(2)), off);
        fill(g, rect(0.25, 0, px(2), px(4)), off);
        fill(g, rect(0, 0.25, px(4), px(2)), off);
        fill(g, rect(-0.25, 0, px(2), px(4)), off);
        fill(g, rect(-px(2), 0, px(3), px(3)), new Color(0xEDEDE6));
        fill(g, rect(px(2), 0, px(3), px(3)), new Color(0xEDEDE6));
        pixel(g, -px(3), -px(1), new Color(0xB82A24));
    }

    record Ramp(Color hl, Color base, Color edge, Color deep) {
        static Ramp of(int hl, int base, int edge, int deep) { return new Ramp(new Color(hl), new Color(base), new Color(edge), new Color(deep)); }
    }

    static final Ramp LIME = Ramp.of(0xC2FE61, 0x82E14A, 0x56935A, 0x3E6E4A);
    static final Ramp PINK = Ramp.of(0xFF937A, 0xF24A64, 0xCE2862, 0x9C1E49);
    static final Ramp ORANGE = Ramp.of(0xFFE0C7, 0xFFA37C, 0xDD8955, 0xA05A38);
    static final Ramp HOT = Ramp.of(0xFFA2CA, 0xFF228F, 0xC8187A, 0x8A1458);
    static final Ramp TEAL = Ramp.of(0xA0F8FF, 0x40D0E8, 0x1A9AB8, 0x126A88);
    static final Ramp GOLD_R = Ramp.of(0xFFF0A0, 0xFFC83A, 0xD8902A, 0x9A5A1E);
    static final Ramp BLUE_R = Ramp.of(0xA8C8FF, 0x5A8AF0, 0x3A5AC8, 0x283C8A);
    static final Ramp RED_R = Ramp.of(0xFF8A7A, 0xE8402E, 0xB82A2A, 0x7A1A22);
    static final Ramp GREEN_R = Ramp.of(0x9AE89A, 0x4AB84A, 0x2E8A3E, 0x1E5A2E);
    static final Ramp YELLOW_R = Ramp.of(0xFFF4A0, 0xFFD83A, 0xE0A82A, 0x9A6A1E);
    static final Ramp WHITE_R = Ramp.of(0xFFFFFF, 0xF1F1F1, 0xC8C8D0, 0x8A8A96);
    static final Ramp GRAY_R = Ramp.of(0xD0D4D8, 0x9AA0A6, 0x6A7076, 0x4A5056);
    static final Ramp WOOD_R = Ramp.of(0xD8A870, 0xA8743E, 0x7A4E2A, 0x50321E);
    static final Ramp CREAM = Ramp.of(0xFFF8E8, 0xEADCBE, 0xBCA884, 0x8A7656);
    static final Ramp INK_R = Ramp.of(0x6A6A78, 0x40404A, 0x2A2A32, 0x18181C);
    static final Ramp BACK = Ramp.of(0x8AA8F0, 0x4A6AC8, 0x34509A, 0x22386A);
    static final Ramp PURPLE = Ramp.of(0xD0A8FF, 0x9A62E0, 0x6A3AB0, 0x48247A);
    static final Ramp JADE = Ramp.of(0xA8D8A0, 0x5A9A5A, 0x3A6E3E, 0x24482A);

    static Color shade(Color c, double k) {
        double f = k < 0 ? 1 + k : k;
        return new Color(Math.min(255, (int) (c.getRed() * f)), Math.min(255, (int) (c.getGreen() * f)), Math.min(255, (int) (c.getBlue() * f)));
    }

    static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    static final class Canvas {
        final int[] px = new int[256];

        void set(int x, int y, Color c) { if (x >= 0 && y >= 0 && x < 16 && y < 16) px[y * 16 + x] = c.getRGB(); }

        void blend(int x, int y, Color c) {
            if (x < 0 || y < 0 || x >= 16 || y >= 16) return;
            int dst = px[y * 16 + x];
            double a = c.getAlpha() / 255.0, da = (dst >>> 24) / 255.0, oa = a + da * (1 - a);
            if (oa == 0) return;
            int r = (int) ((c.getRed() * a + ((dst >> 16) & 255) * da * (1 - a)) / oa);
            int g = (int) ((c.getGreen() * a + ((dst >> 8) & 255) * da * (1 - a)) / oa);
            int b = (int) ((c.getBlue() * a + (dst & 255) * da * (1 - a)) / oa);
            px[y * 16 + x] = ((int) Math.round(oa * 255) << 24) | (r << 16) | (g << 8) | b;
        }

        void shape(boolean[][] m, int ox, int oy, Ramp ramp) {
            int h = m.length, w = m[0].length;
            int[][] d = new int[h][w];
            for (int[] row : d) Arrays.fill(row, 99);
            ArrayDeque<int[]> q = new ArrayDeque<>();
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    if (m[y][x] && (x == 0 || y == 0 || x == w - 1 || y == h - 1 || !m[y - 1][x] || !m[y + 1][x] || !m[y][x - 1] || !m[y][x + 1])) {
                        d[y][x] = 1;
                        q.add(new int[]{x, y});
                    }
            while (!q.isEmpty()) {
                int[] p = q.poll();
                for (int[] dd : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = p[0] + dd[0], ny = p[1] + dd[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h || !m[ny][nx] || d[ny][nx] <= d[p[1]][p[0]] + 1) continue;
                    d[ny][nx] = d[p[1]][p[0]] + 1;
                    q.add(new int[]{nx, ny});
                }
            }
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    if (!m[y][x]) continue;
                    boolean lowEdge = y == h - 1 || x == w - 1 || !m[y + 1][x] || !m[y][x + 1];
                    Color c = d[y][x] == 1 ? (lowEdge ? ramp.deep : ramp.edge) : d[y][x] == 2 ? ramp.base : ramp.hl;
                    set(ox + x, oy + y, c);
                }
        }

        void flat(boolean[][] m, int ox, int oy, Color c) {
            for (int y = 0; y < m.length; y++) for (int x = 0; x < m[0].length; x++) if (m[y][x]) set(ox + x, oy + y, c);
        }

        void glyph(boolean[][] m, int ox, int oy, Color c, Color shade) {
            if (shade != null)
                for (int y = 0; y < m.length; y++)
                    for (int x = 0; x < m[0].length; x++)
                        if (m[y][x]) set(ox + x + 1, oy + y + 1, shade);
            for (int y = 0; y < m.length; y++) for (int x = 0; x < m[0].length; x++) if (m[y][x]) set(ox + x, oy + y, c);
        }

        void image(BufferedImage img, int ox, int oy) {
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int c = img.getRGB(x, y);
                    if ((c >>> 24) != 0) set(ox + x, oy + y, new Color(c, true));
                }
        }

        BufferedImage image() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, px, 0, 16);
            return img;
        }
    }

    static boolean[][] mask(BufferedImage img) {
        boolean[][] m = new boolean[img.getHeight()][img.getWidth()];
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) m[y][x] = (img.getRGB(x, y) >>> 24) != 0;
        return m;
    }

    static boolean[][] digit(int n) {
        boolean[][] full = mask(shop("gui" + n));
        int minX = 16, maxX = -1, minY = 16, maxY = -1;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (full[y][x]) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        boolean[][] m = new boolean[maxY - minY + 1][maxX - minX + 1];
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) m[y - minY][x - minX] = full[y][x];
        return m;
    }

    static boolean[][] rect(int w, int h, boolean round) {
        boolean[][] m = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                m[y][x] = !round || !((x == 0 || x == w - 1) && (y == 0 || y == h - 1));
        return m;
    }

    static boolean[][] circle(int d) {
        boolean[][] m = new boolean[d][d];
        double c = (d - 1) / 2.0, r = d / 2.0;
        for (int y = 0; y < d; y++) for (int x = 0; x < d; x++) m[y][x] = Math.hypot(x - c, y - c) < r - 0.05;
        return m;
    }

    static boolean[][] ring(int d, int t) {
        boolean[][] m = circle(d), inner = circle(d - 2 * t);
        for (int y = 0; y < d - 2 * t; y++) for (int x = 0; x < d - 2 * t; x++) if (inner[y][x]) m[y + t][x + t] = false;
        return m;
    }

    static boolean[][] frameMask(int w, int h, int t) {
        boolean[][] m = rect(w, h, true);
        for (int y = t; y < h - t; y++) for (int x = t; x < w - t; x++) m[y][x] = false;
        return m;
    }

    static boolean[][] sprite(String... rows) {
        boolean[][] m = new boolean[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) m[y][x] = rows[y].charAt(x) == '#';
        return m;
    }

    static boolean[][] text(String s) {
        String[] wideN = {"#..#", "##.#", "#.##", "#..#", "#..#"};
        int w = -1;
        for (char ch : s.toCharArray()) w += ch == 'N' ? 5 : 4;
        boolean[][] m = new boolean[5][w];
        int x = 0;
        for (char ch : s.toCharArray()) {
            if (ch == 'N') {
                for (int y = 0; y < 5; y++) for (int k = 0; k < 4; k++) m[y][x + k] = wideN[y].charAt(k) == '#';
                x += 5;
                continue;
            }
            String bits = FONT.get(ch);
            for (int k = 0; k < 15; k++) if (bits.charAt(k) == '1') m[k / 3][x + k % 3] = true;
            x += 4;
        }
        return m;
    }

    static boolean[][] hanzi(String s) {
        BufferedImage t = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = t.createGraphics();
        crisp(g);
        g.setFont(new Font("Microsoft JhengHei", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString(s, 0, 11);
        g.dispose();
        int minX = 16, maxX = -1, minY = 16, maxY = -1;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                if ((t.getRGB(x, y) >>> 24) > 0) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        boolean[][] m = new boolean[maxY - minY + 1][maxX - minX + 1];
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) m[y - minY][x - minX] = (t.getRGB(x, y) >>> 24) > 0;
        return m;
    }

    static boolean[][] mirror(boolean[][] m) {
        boolean[][] o = new boolean[m.length][m[0].length];
        for (int y = 0; y < m.length; y++) for (int x = 0; x < m[0].length; x++) o[y][x] = m[y][m[0].length - 1 - x];
        return o;
    }

    static int cx(boolean[][] m) { return (16 - m[0].length) / 2; }

    static int cy(boolean[][] m) { return (16 - m.length) / 2; }

    static final Color GLYPH = new Color(0xF1F1F1), GLYPH_SHADE = new Color(0xC2C2CC);

    static final boolean[][] PLAY = sprite("##......", "####....", "######..", "########", "########", "######..", "####....", "##......");
    static final boolean[][] QUEEN = sprite(
            "#...##...#", "##..##..##", "##.####.##", "##########", ".########.", "..######..", "...####...", "..######..", ".########.", "##########");
    static final boolean[][] ROOK = sprite(
            "##.####.##", "##.####.##", "##########", ".########.", "..######..", "..######..", "..######..", "..######..", ".########.", "##########");
    static final boolean[][] BISHOP = sprite(
            "....##....", "...####...", "..###.##..", "..##.###..", "..######..", "...####...", "...####...", "..######..", ".########.", "##########");
    static final boolean[][] KNIGHT = sprite(
            "...###....", "..######..", ".########.", "####.####.", "##..#####.", "....#####.", "...######.", "..#######.", ".########.", "##########");
    static final boolean[][] STAR = sprite(
            "......##......", ".....####.....", ".....####.....", "##############", ".############.", "..##########..", "...########...", "...########...", "..####..####..", "..###....###..", ".##........##.");
    static final boolean[][] HEART_M = sprite(".###.###.", "#########", "#########", "#########", ".#######.", "..#####..", "...###...", "....#....");
    static final boolean[][] DIAMOND_M = sprite("....#....", "...###...", "..#####..", ".#######.", "#########", ".#######.", "..#####..", "...###...", "....#....");
    static final boolean[][] SPADE = sprite("....#....", "...###...", "..#####..", ".#######.", "#########", "#########", ".##.#.##.", "....#....", "..#####..");
    static final boolean[][] CLUB = sprite("...###...", "..#####..", "..#####..", "##.###.##", "#########", "##.###.##", "....#....", "...###...", "..#####..");
    static final boolean[][] ARROW = sprite("....##..", "....###.", "########", "########", "########", "....###.", "....##..");
    static final boolean[][] CROSS = sprite("###....###", "####..####", ".########.", "..######..", "...####...", "..######..", ".########.", "####..####", "###....###");

    static void corners(Canvas c, Ramp r) {
        boolean[][] l = sprite("#####", "#####", "##...", "##...", "##...");
        boolean[][] lr = mirror(l), bl = flipV(l), br = mirror(flipV(l));
        c.shape(l, 0, 0, r);
        c.shape(lr, 11, 0, r);
        c.shape(bl, 0, 11, r);
        c.shape(br, 11, 11, r);
    }

    static boolean[][] flipV(boolean[][] m) {
        boolean[][] o = new boolean[m.length][];
        for (int y = 0; y < m.length; y++) o[y] = m[m.length - 1 - y];
        return o;
    }

    static void glow(Canvas c, Ramp r) {
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 3) c.set(x, y, r.hl);
                else if (d < 5) c.set(x, y, r.base);
                else if (d < 6.5) c.blend(x, y, alpha(r.edge, 160));
                else if (d < 8) c.blend(x, y, alpha(r.edge, 70));
            }
    }

    static void plate(Canvas c, Ramp lamp, boolean glowing) {
        c.shape(rect(16, 8, true), 0, 4, WOOD_R);
        c.shape(rect(9, 4, true), 6, 6, CREAM);
        if (glowing) for (int[] q : new int[][]{{3, 2}, {3, 13}, {0, 2}, {6, 2}, {0, 13}, {6, 13}}) c.blend(q[0], q[1], alpha(lamp.hl, 200));
        c.shape(circle(5), 1, 5, lamp);
    }

    static void botPlate(Canvas c) {
        c.shape(rect(16, 8, true), 0, 4, WOOD_R);
        c.shape(rect(9, 7, true), 6, 3, WHITE_R);
        c.set(8, 5, INK_R.deep); c.set(12, 5, INK_R.deep);
        c.set(7, 7, new Color(0xF28ABE)); c.set(13, 7, new Color(0xF28ABE));
        c.shape(circle(5), 1, 5, CREAM);
    }

    static Ramp face(Ramp r) { return new Ramp(r.base, r.base, r.edge, r.deep); }

    static void plaque(Canvas c, int x, int y, int w, int h, Ramp r) {
        c.shape(rect(w, h, true), x, y, face(r));
        c.flat(rect(w - 2, 1, false), x + 1, y + 1, r.hl);
    }

    static void keyCap(Canvas c, int x, int y, int w, int h, Ramp r) {
        c.shape(rect(w, h, true), x, y + 2, new Ramp(r.edge, r.edge, r.deep, r.deep));
        plaque(c, x, y, w, h, r);
    }

    static void key(Canvas c, Ramp r, String h) {
        keyCap(c, 0, 0, 16, 14, r);
        boolean[][] m = hanzi(h);
        c.glyph(m, cx(m), 1 + (13 - m.length) / 2, GLYPH, r.deep);
    }

    static void card(Canvas c, int x, int y, int w, int h, Ramp face) { c.shape(rect(w, h, true), x, y, face); }

    static void backCard(Canvas c, int x, int y) {
        card(c, x, y, 7, 11, BACK);
        c.flat(frameMask(3, 7, 1), x + 2, y + 2, BACK.hl);
    }

    static void chip(Canvas c, int x, int y, Ramp r) {
        c.shape(rect(6, 3, true), x, y, r);
        c.set(x + 2, y + 1, WHITE_R.hl);
    }

    static void stack(Canvas c, int x, int baseY, int n, Ramp r) {
        for (int k = 0; k < n; k++) chip(c, x, baseY - k * 2, r);
    }

    static void disc(Canvas c, Ramp r, String label) {
        c.shape(circle(16), 0, 0, r);
        c.shape(circle(10), 3, 3, face(r));
        boolean[][] t = text(label);
        c.glyph(t, cx(t), cy(t), r == WHITE_R ? INK_R.base : GLYPH, r == WHITE_R ? null : r.deep);
    }

    static void rod(Canvas c) {
        c.shape(rect(16, 4, true), 0, 6, WHITE_R);
        c.shape(circle(3), 6, 6, RED_R);
    }

    static void sign(Canvas c, Ramp board) {
        c.shape(rect(16, 16, true), 0, 0, board);
    }

    static void trump(Canvas c, boolean[][] pip, Ramp r) {
        card(c, 2, 0, 12, 16, CREAM);
        c.shape(pip, 2 + (12 - pip[0].length) / 2, (16 - pip.length) / 2, r);
    }

    static void penalty(Canvas c, int n, Ramp r) {
        plaque(c, 1, 0, 14, 16, r);
        boolean[][] d = digit(n);
        boolean[][] plus = sprite(".#.", "###", ".#.");
        c.glyph(plus, 3, 6, GLYPH, r.deep);
        c.glyph(d, 7, (16 - d.length) / 2, GLYPH, r.deep);
    }

    static void clock(Canvas c, boolean whiteTurn) {
        c.shape(rect(5, 3, true), 2, whiteTurn ? 3 : 1, whiteTurn ? ORANGE : GRAY_R);
        c.shape(rect(5, 3, true), 9, whiteTurn ? 1 : 3, whiteTurn ? GRAY_R : ORANGE);
        c.shape(rect(16, 11, true), 0, 5, WOOD_R);
        c.shape(circle(6), 1, 7, WHITE_R);
        c.shape(circle(6), 9, 7, INK_R);
        c.set(3, 8, INK_R.deep); c.set(3, 9, INK_R.deep); c.set(11, 9, WHITE_R.hl); c.set(12, 10, WHITE_R.hl);
    }

    static void wind(Canvas c) {
        glow(c, ORANGE);
    }

    static boolean[][] burst() {
        boolean[][] m = new boolean[16][16];
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double a = Math.atan2(y - 7.5, x - 7.5), d = Math.hypot(x - 7.5, y - 7.5);
                m[y][x] = d < ((Math.floorMod((int) Math.floor((a + Math.PI) / (Math.PI / 6)), 2) == 0) ? 8 : 6.4);
            }
        return m;
    }

    static final boolean[][] BNO = sprite("##..#..#.###.#", "#.#.##.#.#.#.#", "##..#.##.#.#.#", "#.#.#..#.#.#..", "##..#..#.###.#");
}
