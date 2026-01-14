package dev.szedann.create_train_physics;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateTrainPhysics.MODID)
public class CreateTrainPhysics {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "create_train_physics";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "create_train_physics" namespace
//    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "create_train_physics" namespace
//    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);


    // Creates a new Block with the id "create_train_physics:example_block", combining the namespace and path
//    public static final DeferredBlock<Block> TRAIN_ENGINE_BLOCK = BLOCKS.registerSimpleBlock("train_engine_block", BlockBehaviour.Properties.of().mapColor(MapColor.METAL));
    // Creates a new BlockItem with the id "create_train_physics:example_block", combining the namespace and path
//    public static final DeferredItem<BlockItem> TRAIN_ENGINE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("train_engine_block", TRAIN_ENGINE_BLOCK);

    public static final TagKey<Block> MOTOR_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CreateTrainPhysics.MODID, "train_motor")
    );

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CreateTrainPhysics(IEventBus modEventBus, ModContainer modContainer) {

//        BLOCKS.register(modEventBus);
//        ITEMS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
//    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey()) event.accept(TRAIN_ENGINE_BLOCK_ITEM);
//    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
//        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
//    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//    public static class ClientModEvents {
//        @SubscribeEvent
//        public static void onClientSetup(FMLClientSetupEvent event) {
//
//        }
//    }
}
