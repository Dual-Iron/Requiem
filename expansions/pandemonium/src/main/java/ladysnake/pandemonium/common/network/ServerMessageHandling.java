package ladysnake.pandemonium.common.network;

import ladysnake.pandemonium.api.anchor.FractureAnchor;
import ladysnake.pandemonium.api.anchor.FractureAnchorManager;
import ladysnake.pandemonium.common.entity.PlayerShellEntity;
import ladysnake.pandemonium.common.impl.anchor.AnchorFactories;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.network.RequiemNetworking;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import static ladysnake.requiem.common.network.RequiemNetworking.ETHEREAL_FRACTURE;
import static ladysnake.requiem.common.network.RequiemNetworking.createEmptyMessage;

public class ServerMessageHandling {
    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(ETHEREAL_FRACTURE, (context, buf) -> context.getTaskQueue().execute(() -> {
            PlayerEntity player = context.getPlayer();
            RemnantComponent remnantState = RemnantComponent.get(player);
            if (remnantState.getRemnantType().isDemon()) {
                PossessionComponent possessionComponent = PossessionComponent.get(player);
                FractureAnchorManager anchorManager = FractureAnchorManager.get(player.world);
                if (!remnantState.isSoul()) {
                    PlayerShellEntity shellEntity = PlayerShellEntity.fromPlayer(player);
                    player.world.spawnEntity(shellEntity);
                    FractureAnchor anchor = anchorManager.addAnchor(AnchorFactories.fromEntityUuid(shellEntity.getUuid()));
                    anchor.setPosition(shellEntity.getX(), shellEntity.getY(), shellEntity.getZ());
                    remnantState.setSoul(true);
                } else if (possessionComponent.isPossessing()/* && state.getAnchor() != null*/) {
                    // TODO make a gamerule to keep the inventory when leaving a mob
                    possessionComponent.stopPossessing();
                } else {
                    return;
                }
                RequiemNetworking.sendTo((ServerPlayerEntity)player, createEmptyMessage(PandemoniumNetworking.ETHEREAL_ANIMATION));
            }
        }));
    }
}
