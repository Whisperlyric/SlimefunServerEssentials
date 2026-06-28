package me.justahuman.slimefun_server_essentials.implementation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.LockedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.justahuman.slimefun_server_essentials.util.JsonUtils;
import me.justahuman.slimefun_server_essentials.util.ReflectionUtils;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 导出 Slimefun 物品组到 {@code slimefun/item_groups/<addon>.json}，
 * 供客户端资源包模式加载，用于 JEI 物品分级展示。
 *
 * 客户端格式见 {@code SlimefunItemGroup.deserialize}：
 * <pre>
 * {
 *   "<groupId>": {
 *     "item": { <serialized item> },
 *     "items": ["<sfItemId>", ...],
 *     "nested": ["<subGroupId>", ...],
 *     "locked": ["<parentGroupId>", ...]
 *   }
 * }
 * </pre>
 */
public final class ItemGroupsExporter {
    private ItemGroupsExporter() {}

    public static void generate() {
        final List<ItemGroup> allGroups = Slimefun.getRegistry().getAllItemGroups();

        // 预构建 NestedItemGroup -> 子组 ID 列表
        final Map<ItemGroup, List<String>> nestedChildren = new LinkedHashMap<>();
        for (ItemGroup group : allGroups) {
            if (group instanceof SubItemGroup sub) {
                final ItemGroup parent = sub.getParent();
                if (parent != null) {
                    final SlimefunAddon subAddon = sub.getAddon();
                    final String subAddonId = subAddon != null
                            ? subAddon.getName().toLowerCase(Locale.ROOT).replace(" ", "_")
                            : "slimefun";
                    nestedChildren.computeIfAbsent(parent, k -> new ArrayList<>())
                            .add(subAddonId + ":" + sub.getKey().getKey());
                }
            }
        }

        // 按 addon 分组
        final Map<String, JsonObject> groupsByAddon = new LinkedHashMap<>();
        for (ItemGroup group : allGroups) {
            final SlimefunAddon addon = group.getAddon();
            if (addon == null) {
                continue;
            }
            final String addonId = addon.getName().toLowerCase(Locale.ROOT).replace(" ", "_");
            final JsonObject groupJson = serializeGroup(group, nestedChildren.getOrDefault(group, Collections.emptyList()));
            groupsByAddon.computeIfAbsent(addonId, k -> new JsonObject())
                    .add(group.getKey().getKey(), groupJson);
        }

        for (Map.Entry<String, JsonObject> entry : groupsByAddon.entrySet()) {
            JsonUtils.generated("slimefun/item_groups/" + entry.getKey(), entry.getValue());
        }
    }

    private static JsonObject serializeGroup(ItemGroup group, List<String> nested) {
        final JsonObject json = new JsonObject();

        // 图标物品：通过反射获取 protected 字段 item
        final ItemStack item = ReflectionUtils.getField(ItemGroup.class, group, "item", null);
        if (item != null && !item.getType().isAir()) {
            json.add("item", JsonUtils.serializeItem(item));
        }

        // 物品列表（FlexItemGroup 没有 getItems()，跳过）
        final JsonArray items = new JsonArray();
        if (!(group instanceof FlexItemGroup)) {
            for (SlimefunItem sfItem : group.getItems()) {
                items.add(sfItem.getId());
            }
        }
        if (!items.isEmpty()) {
            json.add("items", items);
        }

        // 嵌套子组 ID 列表
        if (!nested.isEmpty()) {
            final JsonArray nestedArray = new JsonArray();
            nested.forEach(nestedArray::add);
            json.add("nested", nestedArray);
        }

        // 锁定的父组 ID 列表
        if (group instanceof LockedItemGroup locked) {
            final JsonArray lockedArr = new JsonArray();
            for (ItemGroup parent : locked.getParents()) {
                final String parentAddonId = parent.getAddon() != null
                        ? parent.getAddon().getName().toLowerCase(Locale.ROOT).replace(" ", "_")
                        : "slimefun";
                lockedArr.add("slimefun_essentials:" + parentAddonId + "_" + parent.getKey().getKey());
            }
            if (!lockedArr.isEmpty()) {
                json.add("locked", lockedArr);
            }
        }

        return json;
    }
}
