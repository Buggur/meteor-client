package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.InvUtils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;

public class Baconator extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the animal has to be to be roasted.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents flint and steel from being broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> tickInterval = sgGeneral.add(new IntSetting.Builder()
        .name("tick-interval")
        .defaultValue(5)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to cook.")
            .defaultValue(Utils.asObject2BooleanOpenHashMap(
                EntityType.PIG, EntityType.COW, EntityType.SHEEP,
                EntityType.CHICKEN, EntityType.RABBIT))
            .build()
    );

    private Entity entity;
    private int preSlot;
    private int ticks = 0;

    public Baconator() {
        super(Categories.Misc, "baconator", "Ignites every alive piece of food.");
    }

    @Override
    public void onDeactivate() {
        entity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        entity = null;
        ticks++;
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().getBoolean(entity.getType()) || mc.player.distanceTo(entity) > distance.get()) continue;

            boolean findNewFlintAndSteel = false;
            if (mc.player.inventory.getMainHandStack().getItem() instanceof FlintAndSteelItem) {
                if (antiBreak.get() && mc.player.inventory.getMainHandStack().getDamage() >= mc.player.inventory.getMainHandStack().getMaxDamage() - 1)
                    findNewFlintAndSteel = true;
            } else if (mc.player.inventory.offHand.get(0).getItem() instanceof FlintAndSteelItem) {
                if (antiBreak.get() && mc.player.inventory.offHand.get(0).getDamage() >= mc.player.inventory.offHand.get(0).getMaxDamage() - 1)
                    findNewFlintAndSteel = true;
            } else {
                findNewFlintAndSteel = true;
            }

            boolean foundFlintAndSteel = !findNewFlintAndSteel;
            if (findNewFlintAndSteel) {
                int slot = InvUtils.findItemInHotbar(Items.FLINT_AND_STEEL,
                    itemStack -> (!antiBreak.get() || (antiBreak.get() && itemStack.getDamage() < itemStack.getMaxDamage() - 1)));

                if (slot != -1) {
                    mc.player.inventory.selectedSlot = slot;
                    foundFlintAndSteel = true;
                }
            }

            if (foundFlintAndSteel) {
                this.entity = entity;

                // if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity),
                // Rotations.getPitch(entity), -100, this::interact);
                interact();

                return;
            }
        }
    }

    private void interact() {
        Block block = mc.world.getBlockState(entity.getBlockPos()).getBlock();
        Block bottom = mc.world.getBlockState(entity.getBlockPos().down()).getBlock();
        if (block.is(Blocks.WATER) || bottom.is(Blocks.GRASS_PATH)) return;
        if (block.is(Blocks.GRASS))  mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
        LivingEntity animal = (LivingEntity) entity;

        if (animal.getHealth() < 1) {
            mc.interactionManager.attackBlock(entity.getBlockPos(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().west(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().east(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().north(), Direction.DOWN);
            mc.interactionManager.attackBlock(entity.getBlockPos().south(), Direction.DOWN);
        } else {
            if (ticks >= tickInterval.get()) {
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(
                    entity.getPos().subtract(new Vec3d(0, 1, 0)), Direction.UP, entity.getBlockPos().down(), false));
                ticks = 0;
            }

        }

        mc.player.inventory.selectedSlot = preSlot;
    }
}
