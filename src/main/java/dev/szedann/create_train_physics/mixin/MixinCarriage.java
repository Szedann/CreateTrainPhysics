package dev.szedann.create_train_physics.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import dev.szedann.create_train_physics.accessors.IPhysicsCarriage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;

import static dev.szedann.create_train_physics.CreateTrainPhysics.MOTOR_TAG;


@Mixin(value = Carriage.class, remap = false)
public abstract class MixinCarriage implements IPhysicsCarriage {
    @Shadow
    public abstract CarriageContraptionEntity anyAvailableEntity();

    @Unique
    private @Nullable Integer railways$mass = null;
    @Unique

    private @Nullable Integer trainphys$engineCount = null;

    @Inject(method = "write", at = @At("RETURN"))
    private void writeMassAndEngineCount(DimensionPalette dimensions, HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        Integer mass = railways$getMass();
        if(mass != null)
            tag.putInt("mass", mass);
        Integer engineCount = trainphys$getEngineCount();
        if(engineCount != null)
            tag.putInt("engineCount", engineCount);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void readMassAndEngineCount(CompoundTag tag, HolderLookup.Provider registries, TrackGraph graph, DimensionPalette dimensions, CallbackInfoReturnable<Carriage> cir) {
        IPhysicsCarriage carriage = (IPhysicsCarriage) cir.getReturnValue();

        if(tag.contains("mass", CompoundTag.TAG_INT))
            carriage.railways$setMass(tag.getInt("mass"));
        if(tag.contains("engineCount", CompoundTag.TAG_INT))
            carriage.trainphys$setEngineCount(tag.getInt("engineCount"));
    }

    @Override
    public @Nullable Integer railways$getMass(){
        if(railways$mass == null || railways$mass == 0) {
            CarriageContraptionEntity entity = anyAvailableEntity();
            if(entity != null) railways$mass = entity.getContraption().getBlocks().size();
        }
        return railways$mass;
    }
    @Override
    public void railways$setMass(int mass){
        railways$mass = mass;
    }

    @Override
    public @Nullable Integer trainphys$getEngineCount() {
        if(trainphys$engineCount == null || trainphys$engineCount == 0) {
            CarriageContraptionEntity entity = anyAvailableEntity();
            if(entity != null){
                AtomicInteger engineCount = new AtomicInteger();

                entity.getContraption().getBlocks().forEach((p, b) -> {
                    if(b.state().getBlock().defaultBlockState().is(MOTOR_TAG)){
                        engineCount.getAndIncrement();
                    }
                });
                trainphys$setEngineCount(engineCount.get());
            }
        }
        return trainphys$engineCount;
    }

    @Override
    public void trainphys$setEngineCount(int engineCount) {
        trainphys$engineCount = engineCount;
    }
}
