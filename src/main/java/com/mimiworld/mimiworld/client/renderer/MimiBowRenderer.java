package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mimiworld.mimiworld.client.model.MimiBowModel;
import com.mimiworld.mimiworld.item.MimiBowItem;

import net.minecraft.world.entity.LivingEntity;

/**
 * 通用 3D 弓渲染器：在渲染状态中附带「当前是否正在拉弓」供动画控制器读取。
 */
public class MimiBowRenderer extends GeoItemRenderer<MimiBowItem> {

    /** 当前渲染的这把弓是否正被持有者拉动。 */
    public static final DataTicket<Boolean> DRAWING =
            DataTickets.create("mimi_bow_drawing", Boolean.class);

    public MimiBowRenderer(MimiBowModel model) {
        super(model);
    }

    @Override
    public void captureDefaultRenderState(MimiBowItem animatable, RenderData renderData,
                                          GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, renderData, renderState, partialTick);
        LivingEntity holder = renderData.itemOwner() == null ? null : renderData.itemOwner().asLivingEntity();
        boolean drawing = holder != null && holder.isUsingItem() && holder.getUseItem() == renderData.itemStack();
        renderState.addGeckolibData(DRAWING, drawing);
    }
}
