package me.ellieis.bingo.resourcepack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static me.ellieis.bingo.Bingo.identifier;

public class UiResourceCreator {
    public static final String BASE_MODEL = "minecraft:item/generated";
    public static final String X32_MODEL = "bingo:sgui/button_32";
    public static final String X32_RIGHT_MODEL = "bingo:sgui/button_32_right";

    public static final Style STYLE = Style.EMPTY.withColor(0xFFFFFF).withFont(identifier("gui"));
    private static final String ITEM_TEMPLATE = """
            {
              "parent": "|BASE|",
              "textures": {
                "layer0": "|ID|"
              }
            }
            """.replace(" ", "").replace("\n", "");

    private static final String ITEM_TEMPLATE_OFFSET = """
            {
              "parent": "|BASE|",
              "textures": {
                "layer0": "|ID|"
              },
              "display": {
                  "gui": {
                    "rotation": [ 0, 0, 0 ],
                    "translation": [ |OFFSET|, 0, 0 ],
                    "scale": [ 1, 1, 1 ]
                  }
                }
            }
            """.replace(" ", "").replace("\n", "");

    private static final List<SlicedTexture> VERTICAL_PROGRESS = new ArrayList<>();
    private static final List<SlicedTexture> HORIZONTAL_PROGRESS = new ArrayList<>();
    private static final List<SimpleModel> SIMPLE_MODEL = new ArrayList<>();
    private static final Char2IntMap SPACES = new Char2IntOpenHashMap();
    private static final List<FontTexture> FONT_TEXTURES = new ArrayList<>();
    private static char character = 'a';

    private static final char CHEST_SPACE0 = character++;
    private static final char CHEST_SPACE1 = character++;
    private static final char ANVIL_SPACE0 = character++;
    private static final char ANVIL_SPACE1 = character++;


    public static Supplier<GuiElementBuilder> icon16(String path) {
        var model = genericIconRaw(Items.ALLIUM, path, BASE_MODEL, 0);
        return () -> new GuiElementBuilder(model).setName(Text.empty()).hideDefaultTooltip();
    }

    public static ItemStack genericIconRaw(Item item, String path, String base, int offset) {
        var extra = offset == 0 ? "" : "_offset_" + offset;

        var texturePath = elementPath(path);
        var modelPath = elementPath(path + extra);
        SIMPLE_MODEL.add(new SimpleModel(texturePath, modelPath, base, offset));
        return ItemDisplayElementUtil.getModel(texturePath);
    }

    private static Identifier elementPath(String path) {
        return identifier("sgui/elements/" + path);
    }

    public static Function<Text, Text> background(String path) {
        var builder = new StringBuilder().append(CHEST_SPACE0);
        var c = (character++);
        builder.append(c);
        builder.append(CHEST_SPACE1);

        var texture = new FontTexture(identifier("sgui/" + path), 13, 256, new char[][] { new char[] {c} });

        FONT_TEXTURES.add(texture);
        return new TextBuilders(Text.literal(builder.toString()).setStyle(STYLE));
    }

    public static void setup() {
        SPACES.put(CHEST_SPACE0, -8);
        SPACES.put(CHEST_SPACE1, -168);
        SPACES.put(ANVIL_SPACE0, -60);
        SPACES.put(ANVIL_SPACE1, -119);
        if (false) {
            PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> UiResourceCreator.generateAssets(b::addData));
        }
    }

    private static void generateProgress(BiConsumer<String, byte[]> assetWriter, List<SlicedTexture> list, boolean horizontal) {
        for (var pair : list) {
            var sourceImage = ResourceUtils.getTexture(elementPath(pair.path()));

            var image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            var xw = horizontal ? image.getHeight() : image.getWidth();

            var mult = pair.reverse ? -1 : 1;
            var offset = pair.reverse ? pair.stop + pair.start - 1 : 0;

            for (var y = pair.start; y < pair.stop; y++) {
                var path = elementPath("gen/" + pair.path + "_" + y);
                var pos = offset + y * mult;

                for (var x = 0; x < xw; x++) {
                    if (horizontal) {
                        image.setRGB(pos, x, sourceImage.getRGB(pos, x));
                    } else {
                        image.setRGB(x, pos, sourceImage.getRGB(x, pos));
                    }
                }

                var out = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                assetWriter.accept(AssetPaths.texture(path.getNamespace(), path.getPath() + ".png"), out.toByteArray());
            }
        }
    }

    public static void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        for (var texture : SIMPLE_MODEL) {
            if (texture.offset == 0) {
                assetWriter.accept("assets/" + texture.modelPath.getNamespace() + "/models/" + texture.modelPath.getPath() + ".json",
                        ITEM_TEMPLATE.replace("|ID|", texture.texturePath.toString()).replace("|BASE|", texture.base).getBytes(StandardCharsets.UTF_8));
            } else {
                assetWriter.accept("assets/" + texture.modelPath.getNamespace() + "/models/" + texture.modelPath.getPath() + ".json",
                        ITEM_TEMPLATE_OFFSET.replace("|ID|", texture.texturePath.toString()).replace("|BASE|", texture.base)
                                .replace("|OFFSET|", "" + texture.offset).getBytes(StandardCharsets.UTF_8));
            }
        }

        generateProgress(assetWriter, VERTICAL_PROGRESS, false);
        generateProgress(assetWriter, HORIZONTAL_PROGRESS, true);

        var fontBase = new JsonObject();
        var providers = new JsonArray();

        {
            var spaces = new JsonObject();
            spaces.addProperty("type", "space");
            var advances = new JsonObject();
            SPACES.char2IntEntrySet().stream().sorted(Comparator.comparing(Char2IntMap.Entry::getCharKey)).forEach((c) -> advances.addProperty(Character.toString(c.getCharKey()), c.getIntValue()));
            spaces.add("advances", advances);
            providers.add(spaces);
        }


        FONT_TEXTURES.forEach((entry) -> {
            var bitmap = new JsonObject();
            bitmap.addProperty("type", "bitmap");
            bitmap.addProperty("file", entry.path + ".png");
            bitmap.addProperty("ascent", entry.ascent);
            bitmap.addProperty("height", entry.height);
            var chars = new JsonArray();

            for (var a : entry.chars) {
                var builder = new StringBuilder();
                for (var b : a) {
                    builder.append(b);
                }
                chars.add(builder.toString());
            }

            bitmap.add("chars", chars);
            providers.add(bitmap);
        });

        fontBase.add("providers", providers);

        assetWriter.accept("assets/bingo/font/gui.json", fontBase.toString().getBytes(StandardCharsets.UTF_8));
    }

    private record TextBuilders(Text base) implements Function<Text, Text> {
        @Override
        public Text apply(Text text) {
            return Text.empty().append(base).append(text);
        }
    }

    public record SlicedTexture(String path, int start, int stop, boolean reverse) {};

    public record FontTexture(Identifier path, int ascent, int height, char[][] chars) {};

    public record SimpleModel(Identifier texturePath, Identifier modelPath, String base, int offset) {}
}