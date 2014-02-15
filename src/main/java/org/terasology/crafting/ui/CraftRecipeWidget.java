/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.crafting.ui;

import org.terasology.asset.Assets;
import org.terasology.crafting.system.recipe.hand.CraftProcessDisplay;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.logic.common.DisplayNameComponent;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Rect2i;
import org.terasology.math.Vector2i;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.CoreWidget;
import org.terasology.rendering.nui.UIWidget;
import org.terasology.rendering.nui.layers.ingame.inventory.ItemIcon;
import org.terasology.rendering.nui.widgets.ActivateEventListener;
import org.terasology.rendering.nui.widgets.UIButton;
import org.terasology.world.block.Block;
import org.terasology.world.block.items.BlockItemComponent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class CraftRecipeWidget extends CoreWidget {
    private final CreationCallback callback;

    private List<ItemIcon> ingredientsIcons = new LinkedList<>();

    private ItemIcon result;

    private UIButton button;

    private int leftIndent;

    public CraftRecipeWidget(int leftIndent, EntityRef character,
                             CraftProcessDisplay craftingRecipe, CreationCallback callback) {
        this.leftIndent = leftIndent;
        this.callback = callback;

        for (Map.Entry<Integer, Integer> craftingComponents : craftingRecipe.getComponentSlotAndCount().entrySet()) {
            ItemIcon itemIcon = new ItemIcon();
            EntityRef item = InventoryUtils.getItemAt(character, craftingComponents.getKey());
            ItemComponent itemComp = item.getComponent(ItemComponent.class);
            BlockItemComponent blockItemComp = item.getComponent(BlockItemComponent.class);
            if (itemComp != null && itemComp.renderWithIcon) {
                itemIcon.setIcon(itemComp.icon);
            } else if (blockItemComp != null) {
                itemIcon.setMesh(blockItemComp.blockFamily.getArchetypeBlock().getMesh());
                itemIcon.setMeshTexture(Assets.getTexture("engine:terrain"));
            }
            DisplayNameComponent displayName = item.getComponent(DisplayNameComponent.class);
            if (displayName != null) {
                itemIcon.setTooltip(displayName.name);
            }
            itemIcon.setQuantity(craftingComponents.getValue());

            ingredientsIcons.add(itemIcon);
        }

        result = new ItemIcon();
        Block resultBlock = craftingRecipe.getResultBlock();
        Prefab resultItem = craftingRecipe.getResultItem();
        if (resultBlock != null) {
            result.setMesh(resultBlock.getMesh());
            result.setMeshTexture(Assets.getTexture("engine:terrain"));
        } else if (resultItem != null) {
            result.setIcon(resultItem.getComponent(ItemComponent.class).icon);
            DisplayNameComponent displayName = resultItem.getComponent(DisplayNameComponent.class);
            if (displayName != null) {
                result.setTooltip(displayName.name);
            }
        }
        result.setQuantity(craftingRecipe.getResultQuantity());


        button = new UIButton();
        button.setText("Craft 1");
        button.subscribe(
                new ActivateEventListener() {
                    @Override
                    public void onActivated(UIWidget widget) {
                        produceOne();
                    }
                }
        );
    }

    private void produceOne() {
        callback.createOne();
    }

    @Override
    public void onDraw(Canvas canvas) {
        int x = leftIndent;
        Vector2i size = canvas.size();
        for (ItemIcon ingredientsIcon : ingredientsIcons) {
            Vector2i iconSize = canvas.calculatePreferredSize(ingredientsIcon);
            canvas.drawWidget(ingredientsIcon, Rect2i.createFromMinAndSize(x, 0, iconSize.x, iconSize.y));
            x += iconSize.x;
        }


        Vector2i resultSize = canvas.calculatePreferredSize(result);
        Vector2i buttonSize = canvas.calculatePreferredSize(button);

        canvas.drawWidget(result, Rect2i.createFromMinAndSize(size.x - resultSize.x - buttonSize.x - 5, 0, resultSize.x, resultSize.y));
        canvas.drawWidget(button, Rect2i.createFromMinAndSize(size.x - buttonSize.x, (size.y - buttonSize.y) / 2, buttonSize.x, buttonSize.y));
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        int maxX = canvas.size().x;
        int maxY = 0;
        for (ItemIcon ingredientsIcon : ingredientsIcons) {
            maxY = Math.max(maxY, canvas.calculatePreferredSize(ingredientsIcon).y);
        }
        maxY = Math.max(maxY, canvas.calculatePreferredSize(result).y);
        maxY = Math.max(maxY, canvas.calculatePreferredSize(button).y);

        return new Vector2i(maxX, maxY);
    }

    @Override
    public Vector2i getMaxContentSize(Canvas canvas) {
        return getPreferredContentSize(canvas, null);
    }
}