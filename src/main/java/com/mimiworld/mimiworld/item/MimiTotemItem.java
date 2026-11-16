package com.mimiworld.mimiworld.item;

import net.minecraft.world.item.Item;

/**
 * 米米图腾（Mimi Totem）：与原版不死图腾行为一致的免死道具。
 *
 * <p>免死触发逻辑见 {@link com.mimiworld.mimiworld.event.MimiTotemEvents}：
 * 主手/副手持有米米图腾受到致命伤害时，恢复 1 点生命并清除原有效果，
 * 附加生命恢复 II 45s、伤害吸收 II 5s、抗火 I 40s，同时播放图腾粒子/音效。</p>
 */
public class MimiTotemItem extends Item {
    public MimiTotemItem(Properties properties) {
        super(properties);
    }
}
