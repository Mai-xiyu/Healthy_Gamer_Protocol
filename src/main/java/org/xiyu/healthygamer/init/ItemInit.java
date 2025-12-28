package org.xiyu.healthygamer.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.xiyu.healthygamer.HealthyGamerMod;

public class ItemInit {
    // 创建一个推迟注册器 (DeferredRegister)
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HealthyGamerMod.MODID);

    // 注册“爷爷的身份证”
    public static final RegistryObject<Item> GRANDPA_CARD = ITEMS.register("grandpa_id_card",
            () -> new Item(new Item.Properties()
                    .stacksTo(1)       // 不可堆叠
                    .rarity(Rarity.EPIC) // 紫色史诗品质
                    .fireResistant())); // 防火 (爷爷的意志不可磨灭！)

    // 注册方法
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}