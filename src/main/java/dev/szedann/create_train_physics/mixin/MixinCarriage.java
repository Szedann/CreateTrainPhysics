package dev.szedann.create_train_physics.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.szedann.create_train_physics.Create_train_physics;
import dev.szedann.create_train_physics.accessors.IPhysicsCarriage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = Carriage.class, remap = false)
public abstract class MixinCarriage implements IPhysicsCarriage {
    @Shadow
    public abstract CarriageContraptionEntity anyAvailableEntity();

    @Unique
    private @Nullable Integer railways$mass = null;
    @Unique

    private @Nullable Integer trainphys$engineCount = null;

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
                    if(b.state().getBlock().defaultBlockState().is(Create_train_physics.TRAIN_ENGINE_BLOCK)){
                        engineCount.getAndIncrement();
                    }
                });
                trainphys$engineCount = engineCount.get();
            }
        }
        return trainphys$engineCount;
    }

    @Override
    public void trainphys$setEngineCount(int engineCount) {

    }
}
