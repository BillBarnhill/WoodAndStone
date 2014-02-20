/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.crafting.system.recipe.hand;

import org.terasology.asset.Assets;
import org.terasology.crafting.system.recipe.behaviour.IngredientCraftBehaviour;
import org.terasology.crafting.system.recipe.render.CraftIngredientRenderer;
import org.terasology.crafting.system.recipe.render.ItemSlotIngredientRenderer;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class CompositeTypeBasedCraftInHandRecipe implements CraftInHandRecipe {
    private List<IngredientCraftBehaviour> itemCraftBehaviours = new ArrayList<>();
    private String prefabName;
    private boolean block;

    public CompositeTypeBasedCraftInHandRecipe(String resultPrefab, boolean block) {
        this.prefabName = resultPrefab;
        this.block = block;
    }

    public void addItemCraftBehaviour(IngredientCraftBehaviour itemCraftBehaviour) {
        itemCraftBehaviours.add(itemCraftBehaviour);
    }

    @Override
    public List<CraftInHandResult> getMatchingRecipeResults(EntityRef character) {
        // TODO: Improve searching for different kinds of items of the same type in whole inventory, not just first matching
        int[] slots = new int[itemCraftBehaviours.size()];
        int maxMultiplier = Integer.MAX_VALUE;
        for (int i = 0; i < itemCraftBehaviours.size(); i++) {
            int matchingSlot = findMatchingSlot(character, itemCraftBehaviours.get(i));
            if (matchingSlot == -1) {
                return null;
            }
            maxMultiplier = Math.min(maxMultiplier, itemCraftBehaviours.get(i).getMaxMultiplier(character, matchingSlot));
            slots[i] = matchingSlot;
        }

        return Collections.<CraftInHandResult>singletonList(new CraftResult(slots, maxMultiplier));
    }

    private int findMatchingSlot(EntityRef character, IngredientCraftBehaviour itemCraftBehaviour) {
        int slotCount = InventoryUtils.getSlotCount(character);
        for (int i = 0; i < slotCount; i++) {
            if (itemCraftBehaviour.isValidToCraft(character, i, 1)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public CraftInHandResult getResultById(EntityRef character, String resultId) {
        String[] slots = resultId.split("\\|");
        int[] slotsNo = new int[slots.length - 1];
        int maxMultiplier = Integer.parseInt(slots[slots.length - 1]);
        for (int i = 0; i < slots.length - 1; i++) {
            slotsNo[i] = Integer.parseInt(slots[i]);
        }
        return new CraftResult(slotsNo, maxMultiplier);
    }

    public class CraftResult implements CraftInHandResult {
        private int[] slots;
        private int maxMultiplier;
        private List<CraftIngredientRenderer> renderers;

        public CraftResult(int[] slots, int maxMultiplier) {
            this.slots = slots;
            this.maxMultiplier = maxMultiplier;
        }

        @Override
        public String getResultId() {
            StringBuilder sb = new StringBuilder();
            for (int slot : slots) {
                sb.append(slot).append("|");
            }

            return sb.append(maxMultiplier).toString();
        }

        @Override
        public List<CraftIngredientRenderer> getIngredients(EntityRef entity) {
            if (renderers == null) {
                renderers = new LinkedList<>();
                for (int i = 0; i < slots.length; i++) {
                    renderers.add(new ItemSlotIngredientRenderer(entity, slots[i], itemCraftBehaviours.get(i).getCountBasedOnMultiplier()));
                }
            }
            return renderers;
        }

        @Override
        public int getMaxMultiplier() {
            return maxMultiplier;
        }

        @Override
        public int getResultQuantity() {
            return 1;
        }

        @Override
        public Block getResultBlock() {
            if (block) {
                return CoreRegistry.get(BlockManager.class).getBlock(prefabName);
            }
            return null;
        }

        @Override
        public Prefab getResultItem() {
            if (!block) {
                return Assets.getPrefab(prefabName);
            }
            return null;
        }

        private EntityRef createResult(int multiplier) {
            if (block) {
                BlockFamily blockFamily = CoreRegistry.get(BlockManager.class).getBlockFamily(prefabName);
                return new BlockItemFactory(CoreRegistry.get(EntityManager.class)).newInstance(blockFamily, multiplier);
            } else {
                EntityRef entityRef = CoreRegistry.get(EntityManager.class).create(prefabName);
                ItemComponent item = entityRef.getComponent(ItemComponent.class);
                item.stackCount = (byte) multiplier;
                entityRef.saveComponent(item);
                return entityRef;
            }
        }

        @Override
        public EntityRef craft(EntityRef character, int count) {
            InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
            for (int i = 0; i < slots.length; i++) {
                if (!itemCraftBehaviours.get(i).isValidToCraft(character, slots[i], count)) {
                    return EntityRef.NULL;
                }
            }

            for (int i = 0; i < slots.length; i++) {
                itemCraftBehaviours.get(i).processIngredient(character, character, slots[i], count);
            }

            return createResult(count);
        }
    }
}
