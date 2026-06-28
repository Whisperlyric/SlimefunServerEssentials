package me.justahuman.slimefun_server_essentials.implementation;

import com.google.gson.JsonObject;
import me.justahuman.slimefun_server_essentials.api.display.ComponentType;
import me.justahuman.slimefun_server_essentials.api.display.SimpleRenderable;
import me.justahuman.slimefun_server_essentials.util.JsonUtils;

import java.util.Map;

/**
 * 导出配方标签到 {@code slimefun/labels/<addon>.json}，供客户端资源包模式加载。
 *
 * 客户端格式见 {@code SlimefunLabel.deserialize}：
 * <pre>
 * {
 *   "<labelId>": {
 *     "LIGHT": {"u": 57, "v": 0, "width": 13, "height": 13},
 *     "DARK":   {"u": 57, "v": 0, "width": 13, "height": 13}
 *   }
 * }
 * </pre>
 *
 * 仅导出基础 {@link ComponentType}（非 FillingComponentType）作为标签，
 * 例如 {@code day}、{@code night}。填充型组件不作为标签使用。
 */
public final class LabelsExporter {
    private LabelsExporter() {}

    public static void generate() {
        final JsonObject labels = new JsonObject();
        for (Map.Entry<String, ComponentType> entry : DisplayComponentTypes.getComponentTypes().entrySet()) {
            final ComponentType type = entry.getValue();
            // 跳过 FillingComponentType，仅导出简单 ComponentType 作为标签
            if (!(type.light() instanceof SimpleRenderable light)
                    || !(type.dark() instanceof SimpleRenderable dark)) {
                continue;
            }

            final JsonObject labelObj = new JsonObject();
            labelObj.add("LIGHT", modeJson(light));
            labelObj.add("DARK", modeJson(dark));
            labels.add(entry.getKey(), labelObj);
        }

        // 所有标签放在一个 slimefun 命名空间文件中
        JsonUtils.generated("slimefun/labels/slimefun", labels);
    }

    private static JsonObject modeJson(SimpleRenderable renderable) {
        final JsonObject mode = new JsonObject();
        mode.addProperty("u", renderable.u());
        mode.addProperty("v", renderable.v());
        mode.addProperty("width", renderable.width());
        mode.addProperty("height", renderable.height());
        return mode;
    }
}
