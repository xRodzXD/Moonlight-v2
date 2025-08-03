package wtf.moonlight.module.impl.player;

import lombok.Getter;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.DamageSource;
import com.cubk.EventTarget;
import wtf.moonlight.events.packet.PacketEvent;
import wtf.moonlight.events.player.AttackEvent;
import wtf.moonlight.events.player.MotionEvent;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.Categor;
import wtf.moonlight.module.ModuleInfo;
import wtf.moonlight.module.impl.movement.InvMove;
import wtf.moonlight.module.impl.movement.Scaffold;
import wtf.moonlight.module.values.impl.BoolValue;
import wtf.moonlight.module.values.impl.ListValue;
import wtf.moonlight.module.values.impl.SliderValue;
import wtf.moonlight.util.MathUtil;
import wtf.moonlight.util.TimerUtil;
import wtf.moonlight.util.packet.PacketUtils;
import wtf.moonlight.util.player.InventoryUtil;
import wtf.moonlight.util.player.PlayerUtil;
import wtf.moonlight.component.SelectorDetectionComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@ModuleInfo(name = "InvManager", category = Categor.Player)
public class InvManager extends Module {
    private final ListValue modeValue = new ListValue("Mode", new String[]{"Basic", "OpenInv"},  "Basic", this);
    private final SliderValue delay = new SliderValue("Delay", 150, 0, 500, this);

    private final BoolValue autoArmor = new BoolValue("AutoArmor", true, this);
    private final BoolValue dropItems = new BoolValue("Drop Items", true, this);

    public final BoolValue keepBucket = new BoolValue("Keep Bucket", true, this);
    public final BoolValue keepOtherFood = new BoolValue("Keep Other Food", false, this);
    public final BoolValue keepProjectiles = new BoolValue("Keep Projectiles", false, this);
    public final BoolValue keepFishingRod = new BoolValue("Keep Fishing Rod", false, this);

    private final SliderValue swordSlot = new SliderValue("Sword Slot", 1, 0, 9, this);
    private final SliderValue throwableSlot = new SliderValue("Throwable Slot", 2, 0, 9, this);
    private final SliderValue gappleSlot = new SliderValue("Gapple Slot", 3, 0, 9, this);
    private final SliderValue blockSlot = new SliderValue("Block Slot", 4, 0, 9, this);
    private final SliderValue bucketSlot = new SliderValue("Bucket Slot", 7, 0, 9, this);
    private final SliderValue potionSlot = new SliderValue("Potion Slot", 8, 0, 9, this);
    private final SliderValue pickaxeSlot = new SliderValue("Pickaxe Slot", 8, 0, 9, this);
    private final SliderValue axeSlot = new SliderValue("Axe Slot", 9, 0, 9, this);

    @Getter
    private boolean moved, open;
    private long nextClick;
    public short action;

    public final TimerUtil timerUtil = new TimerUtil();
    private int chestTicks, attackTicks, placeTicks;

    @Override
    public void onDisable() {
        if (this.canOpenInventory()) {
            this.closeInventory();
        }
        super.onDisable();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        this.attackTicks = 0;
    }

    @EventTarget
    public void onPacketSend(PacketEvent event) {
        if (event.getState() == PacketEvent.State.OUTGOING) {
            if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                this.placeTicks = 0;
            }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            if (mc.thePlayer.ticksExisted <= 40) return;

            if (mc.currentScreen instanceof GuiChest) {
                this.chestTicks = 0;
            } else {
                this.chestTicks++;
            }

            this.moved = false;

            this.attackTicks++;
            this.placeTicks++;

            if (!this.timerUtil.hasTimeElapsed(this.nextClick) || this.chestTicks < 10 || this.attackTicks < 10 || this.placeTicks < 10) {
                this.closeInventory();
                return;
            }

            if (modeValue.is("OpenInv") && !(mc.currentScreen instanceof GuiInventory)) {
                return;
            }

            int INVENTORY_SLOTS = 4 * 9 + 4;
            int throwable = -1, bucket = -1;
            int helmet = -1, chestplate = -1, leggings = -1, boots = -1;
            int sword = -1, pickaxe = -1, axe = -1, block = -1, potion = -1, food = -1;

            Set<Integer> keepSlots = new HashSet<>();

            for (int i = 0; i < INVENTORY_SLOTS; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack == null) continue;

                Item item = stack.getItem();

                if (!InventoryUtil.isValid(stack)) continue;

                if (autoArmor.get() && item instanceof ItemArmor armor) {
                    int reduction = armorReduction(stack);
                    switch (armor.armorType) {
                        case 0:
                            if (helmet == -1 || reduction > armorReduction(mc.thePlayer.inventory.getStackInSlot(helmet))) helmet = i;
                            break;
                        case 1:
                            if (chestplate == -1 || reduction > armorReduction(mc.thePlayer.inventory.getStackInSlot(chestplate))) chestplate = i;
                            break;
                        case 2:
                            if (leggings == -1 || reduction > armorReduction(mc.thePlayer.inventory.getStackInSlot(leggings))) leggings = i;
                            break;
                        case 3:
                            if (boots == -1 || reduction > armorReduction(mc.thePlayer.inventory.getStackInSlot(boots))) boots = i;
                            break;
                    }
                    continue;
                }

                if (item instanceof ItemSpade) continue;

                if (item instanceof ItemSword) {
                    float swordScore = InventoryUtil.calculateSwordScore(stack);
                    int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);

                    if (sword == -1) {
                        sword = i;
                    } else {
                        ItemStack currentBest = mc.thePlayer.inventory.getStackInSlot(sword);
                        float bestScore = InventoryUtil.calculateSwordScore(currentBest);
                        int currentFireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, currentBest);

                        if (fireAspect > 0 && currentFireAspect == 0) {
                            sword = i;
                        } else if (fireAspect > 0 && currentFireAspect > 0 && swordScore > bestScore) {
                            sword = i;
                        } else if (fireAspect == 0 && currentFireAspect == 0 && swordScore > bestScore) {
                            sword = i;
                        }
                    }
                    continue;
                }

                if (item instanceof ItemPickaxe) {
                    if (pickaxe == -1 || InventoryUtil.mineSpeed(stack) > InventoryUtil.mineSpeed(mc.thePlayer.inventory.getStackInSlot(pickaxe))) pickaxe = i;
                    continue;
                }

                if (item instanceof ItemAxe) {
                    if (axe == -1 || InventoryUtil.mineSpeed(stack) > InventoryUtil.mineSpeed(mc.thePlayer.inventory.getStackInSlot(axe))) axe = i;
                    continue;
                }

                if (item instanceof ItemBlock) {
                    keepSlots.add(i);

                    if (block == -1) {
                        block = i;
                    } else {
                        ItemStack currentBlock = mc.thePlayer.inventory.getStackInSlot(block);
                        if (stack.stackSize > currentBlock.stackSize) {
                            block = i;
                        } else if (stack.stackSize == currentBlock.stackSize) {
                            if (Item.getIdFromItem(stack.getItem()) > Item.getIdFromItem(currentBlock.getItem())) {
                                block = i;
                            }
                        }
                    }
                    continue;
                }

                if (item instanceof ItemPotion potionItem) {
                    if (potion == -1) potion = i;
                    else {
                        int curRank = PlayerUtil.potionRanking(((ItemPotion) mc.thePlayer.inventory.getStackInSlot(potion).getItem()).getEffects(mc.thePlayer.inventory.getStackInSlot(potion)).get(0).getPotionID());
                        int newRank = PlayerUtil.potionRanking(potionItem.getEffects(stack).get(0).getPotionID());
                        if (newRank > curRank) potion = i;
                    }
                    continue;
                }

                if (item instanceof ItemFood itemFood) {
                    boolean isGoldenApple = item == Item.getItemById(322) || item == Item.getItemById(466);

                    if (isGoldenApple || keepOtherFood.get()) {
                        keepSlots.add(i);

                        if (food == -1) {
                            food = i;
                        } else {
                            ItemStack currentBestStack = mc.thePlayer.inventory.getStackInSlot(food);
                            float curSat = ((ItemFood) currentBestStack.getItem()).getSaturationModifier(currentBestStack);
                            float newSat = itemFood.getSaturationModifier(stack);

                            if (newSat > curSat) {
                                food = i;
                            }
                        }
                    }
                }
            }

            Stream.of(helmet, chestplate, leggings, boots, sword, pickaxe, axe, block, potion, food).filter(slot -> slot != -1).forEach(keepSlots::add);

            for (int i = 0; i < INVENTORY_SLOTS; i++) {
                if (!keepSlots.contains(i)) {
                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (stack == null) continue;
                    Item item = stack.getItem();

                    if (item instanceof ItemBucket) {
                        if (bucket == -1) {
                            bucket = i;
                        }
                        continue;
                    }

                    if (item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl) {
                        if (throwable == -1 || stack.stackSize > mc.thePlayer.inventory.getStackInSlot(throwable).stackSize) {
                            throwable = i;
                        }
                        continue;
                    }

                    if (item instanceof ItemSword) {
                        int durability = stack.getMaxDamage() - stack.getItemDamage();
                        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);

                        if (durability < 30 && fireAspect == 0) {
                            throwItem(i);
                        } else if (i != sword) {
                            throwItem(i);
                        }
                        continue;
                    }

                    if (item instanceof ItemSpade || !InventoryUtil.isValid(stack)) {
                        throwItem(i);
                    } else if (item instanceof ItemFood && item != Item.getItemById(322)
                            && item != Item.getItemById(466)
                            && !keepOtherFood.get()) {
                        throwItem(i);
                    }
                }
            }

            if (autoArmor.get()) {
                if (helmet != -1 && helmet != 39) equipItem(helmet);
                if (chestplate != -1 && chestplate != 38) equipItem(chestplate);
                if (leggings != -1 && leggings != 37) equipItem(leggings);
                if (boots != -1 && boots != 36) equipItem(boots);
            }

            if (sword != -1) this.moveItemToSlot(sword, swordSlot);
            if (pickaxe != -1) this.moveItemToSlot(pickaxe, pickaxeSlot);
            if (axe != -1) this.moveItemToSlot(axe, axeSlot);
            if (potion != -1) this.moveItemToSlot(potion, potionSlot);
            if (food != -1) this.moveItemToSlot(food, gappleSlot);
            if (throwable != -1) this.moveItemToSlot(throwable, throwableSlot);
            if (bucket != -1) this.moveItemToSlot(bucket, bucketSlot);

            if (block != -1 && blockSlot.getValue() > 0 && block != blockSlot.getValue()-1 && !isEnabled(Scaffold.class)) {
                ItemStack currentSlot = mc.thePlayer.inventory.getStackInSlot((int)(blockSlot.getValue()-1));
                if (currentSlot == null || !ItemStack.areItemStacksEqual(
                        mc.thePlayer.inventory.getStackInSlot(block),
                        currentSlot)) {
                    moveItem(block, (int)(blockSlot.getValue()-37));
                }
            }

            if (canOpenInventory() && !moved) closeInventory();
        }
    }

    private boolean canOpenInventory() {
        return isEnabled(InvMove.class) && !(mc.currentScreen instanceof GuiInventory);
    }

    private void moveItemToSlot(int itemIndex, SliderValue slotSetting) {
        if (slotSetting.getValue() > 0 && itemIndex != slotSetting.getValue()-1) {
            moveItem(itemIndex, (int)(slotSetting.getValue()-37));
        }
    }

    private void openInventory() {
        if (!this.open) {
            PacketUtils.sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            this.open = true;
        }
    }

    private void throwItem(final int slot) {
        if ((!this.moved || this.nextClick <= 0) && !SelectorDetectionComponent.selector(slot) && dropItems.get()) {
            if (this.canOpenInventory()) openInventory();
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, this.slot(slot), 1, 4, mc.thePlayer);
            this.updateNextClick();
        }
    }

    private void moveItem(int slot, int destination) {
        if ((!this.moved || this.nextClick <= 0) && !SelectorDetectionComponent.selector(slot)) {
            if (this.canOpenInventory()) openInventory();
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, this.slot(slot), this.slot(destination), 2, mc.thePlayer);
            this.updateNextClick();
        }
    }

    private void equipItem(int slot) {
        if ((!this.moved || this.nextClick <= 0) && !SelectorDetectionComponent.selector(slot) && autoArmor.get()) {
            if (this.canOpenInventory()) openInventory();
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, this.slot(slot), 0, 1, mc.thePlayer);
            this.updateNextClick();
        }
    }

    private void updateNextClick() {
        this.nextClick = Math.round((float) MathUtil.getRandom(this.delay.getValue().intValue(), this.delay.getValue().intValue()));
        this.timerUtil.reset();
        this.moved = true;
    }

    private void closeInventory() {
        if (this.open) {
            PacketUtils.sendPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            this.open = false;
        }
    }

    private int armorReduction(ItemStack stack) {
        ItemArmor armor = (ItemArmor) stack.getItem();
        return armor.damageReduceAmount + EnchantmentHelper.getEnchantmentModifierDamage(new ItemStack[]{stack}, DamageSource.generic);
    }

    private int slot(final int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        }

        if (slot < 9) {
            return slot + 36;
        }

        return slot;
    }
}