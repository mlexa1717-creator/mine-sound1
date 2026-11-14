package local.phrasecombo.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Unique private long phrasecombo$last;
    @Unique private int phrasecombo$index;
    @Unique private Object phrasecombo$world;
    @Unique private SoundInstance phrasecombo$previous;

    @ModifyVariable(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
        at = @At("HEAD"), argsOnly = true)
    private SoundInstance phrasecombo$replace(SoundInstance original) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return original;
        if (phrasecombo$world != client.world) {
            phrasecombo$world = client.world;
            phrasecombo$last = 0;
            phrasecombo$index = 0;
            phrasecombo$previous = null;
        }
        var id = original.getId();
        if (!id.getNamespace().equals("minecraft")) return original;
        String path = id.getPath();
        if (!(path.equals("entity.player.attack.strong") || path.equals("entity.player.attack.weak")
            || path.equals("entity.player.attack.crit") || path.equals("entity.player.attack.sweep"))) return original;
        double dx = original.getX() - client.player.getX();
        double dy = original.getY() - client.player.getY();
        double dz = original.getZ() - client.player.getZ();
        // Vanilla attack sounds originate at the attacker's position.
        // This is a proximity heuristic, not server-confirmed attacker identification.
        if (original.isRelative() || dx*dx + dy*dy + dz*dz > 0.36) return original;
        long now = System.nanoTime();
        if (phrasecombo$last != 0 && now - phrasecombo$last < 50_000_000L) {
            return new PositionedSoundInstance(Identifier.of("phrasecombo", "silent"),
                SoundCategory.PLAYERS, 0f, 1f, SoundInstance.createRandom(), false, 0,
                SoundInstance.AttenuationType.NONE, 0, 0, 0, true);
        }
        if (phrasecombo$last == 0 || now - phrasecombo$last >= 1_100_000_000L) phrasecombo$index = 0;
        phrasecombo$last = now;
        if (phrasecombo$previous != null) client.getSoundManager().stop(phrasecombo$previous);
        SoundInstance replacement = new PositionedSoundInstance(
            Identifier.of("phrasecombo", "hit" + (phrasecombo$index + 1)),
            SoundCategory.PLAYERS, 1f, 1f, SoundInstance.createRandom(), false, 0,
            SoundInstance.AttenuationType.NONE, 0, 0, 0, true);
        phrasecombo$index = (phrasecombo$index + 1) % 3;
        phrasecombo$previous = replacement;
        return replacement;
    }
}
