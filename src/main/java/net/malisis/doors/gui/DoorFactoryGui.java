/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.doors.gui;

import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

import net.malisis.core.client.gui.ComponentPosition;
import net.malisis.core.client.gui.GuiTexture;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UISlot;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.container.UIPlayerInventory;
import net.malisis.core.client.gui.component.container.UITabGroup;
import net.malisis.core.client.gui.component.decoration.UIImage;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.decoration.UITooltip;
import net.malisis.core.client.gui.component.element.Position;
import net.malisis.core.client.gui.component.element.Size;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UIRadioButton;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.component.interaction.UITab;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.event.ComponentEvent.ValueChange;
import net.malisis.core.client.gui.event.component.StateChangeEvent.ActiveStateChange;
import net.malisis.core.client.gui.render.TexturedBackground.WindowBackground;
import net.malisis.core.inventory.MalisisInventoryContainer;
import net.malisis.core.renderer.icon.Icon;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.doors.DoorDescriptor.RedstoneBehavior;
import net.malisis.doors.DoorRegistry;
import net.malisis.doors.MalisisDoors;
import net.malisis.doors.network.DoorFactoryMessage;
import net.malisis.doors.tileentity.DoorFactoryTileEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * @author Ordinastie
 *
 */
public class DoorFactoryGui extends MalisisGui
{
	public static ResourceLocation tabIconsRl = new ResourceLocation(MalisisDoors.modid, "textures/gui/doorfactory_tabicons.png");
	public static GuiTexture tabTexture = new GuiTexture(tabIconsRl, 128, 128);
	public static Icon propIcon = tabTexture.getIcon(0, 0, 64, 64);
	public static Icon matIcon = tabTexture.getIcon(64, 0, 64, 64);
	public static Icon dcIcon = tabTexture.getIcon(0, 64, 64, 64);

	private DoorFactoryTileEntity tileEntity;
	private UISelect<String> selDoorMovement;
	private UITextField tfOpenTime;
	private UITextField tfAutoCloseTime;
	private UICheckBox cbDoubleDoor;
	private UICheckBox cbProximity;
	private UISelect<RedstoneBehavior> selRedstone;
	private UISelect<String> selDoorSound;
	private UIRadioButton rbCreate;
	private UIRadioButton rbEdit;
	private UIContainer<?> contCreate;
	private UIContainer<?> contEdit;
	private UIButton btnCreate;
	private Digicode digicode;

	private static String activeTab;

	public DoorFactoryGui(DoorFactoryTileEntity te, MalisisInventoryContainer container)
	{
		setInventoryContainer(container);
		tileEntity = te;
	}

	@Override
	public void construct()
	{
		UIContainer<?> window = new UIContainer<>(this, "tile.door_factory.name", Size.of(UIPlayerInventory.INVENTORY_SIZE.width() + 80, 255));
		window.setPosition(Position.centered().middleAligned());
		window.setBackground(new WindowBackground(this));

		UIContainer<?> propContainer = getPropertiesContainer();
		UIContainer<?> matContainer = getMaterialsContainer();
		UIContainer<?> dcContainer = getDigicodeContainer();

		UITabGroup tabGroup = new UITabGroup(this, ComponentPosition.LEFT);
		tabGroup.setPosition(Position.of(0, 10));

		int a = 16;
		UIImage propImage = new UIImage(this, tabTexture, propIcon);
		propImage.setSize(Size.of(a, a));
		UITab tabProp = new UITab(this, propImage).setName("tab_prop");
		tabProp.setTooltip(new UITooltip(this, "gui.door_factory.tab_properties")).register(this);
		UIImage matImage = new UIImage(this, tabTexture, matIcon);
		matImage.setSize(Size.of(a, a));
		UITab tabMat = new UITab(this, matImage).setName("tab_mat");
		tabMat.setTooltip(new UITooltip(this, "gui.door_factory.tab_materials")).register(this);
		UIImage dcImage = new UIImage(this, tabTexture, dcIcon);
		dcImage.setSize(Size.of(a, a));
		UITab tabDc = new UITab(this, dcImage).setName("tab_dc");
		tabDc.setTooltip(new UITooltip(this, "gui.door_factory.tab_digicode")).register(this);

		tabGroup.addTab(tabProp, propContainer);
		tabGroup.addTab(tabMat, matContainer);
		tabGroup.addTab(tabDc, dcContainer);

		tabGroup.setActiveTab(activeTab != null ? activeTab : "tab_prop");
		tabGroup.attachTo(window, false);

		btnCreate = new UIButton(this, "gui.door_factory.create_door");
		btnCreate.setSize(Size.of(80, 20));
		btnCreate.setPosition(Position.centered().y(110));
		btnCreate.register(this);
		UISlot outputSlot = new UISlot(this, tileEntity.outputSlot);
		outputSlot.setPosition(Position.centered().y(132));

		UIPlayerInventory playerInv = new UIPlayerInventory(this, inventoryContainer.getPlayerInventory());

		window.add(playerInv);

		window.add(propContainer);
		window.add(matContainer);
		window.add(dcContainer);

		window.add(btnCreate);
		window.add(outputSlot);

		addToScreen(tabGroup);
		addToScreen(window);

		TileEntityUtils.linkTileEntityToGui(tileEntity, this);
	}

	private UIContainer<?> getPropertiesContainer()
	{
		UIContainer<?> propContainer = new UIContainer<>(this, Size.relativeWidth(1.0F).height(95));
		propContainer.setPosition(Position.of(0, 15));

		//Door movement
		int y = 2;
		selDoorMovement = new UISelect<>(this, 100, getSortedList(DoorRegistry.listMovements().keySet(), "door_movement."));
		selDoorMovement.setPosition(Position.rightAligned().y(y));
		selDoorMovement.setLabelPattern("door_movement.%s").register(this);
		UILabel lblDoorMovement = new UILabel(this, "gui.door_factory.door_movement");
		lblDoorMovement.setPosition(Position.of(0, y + 2));
		propContainer.add(lblDoorMovement);
		propContainer.add(selDoorMovement);

		//Opening time
		y += 12;
		tfOpenTime = new UITextField(this, null);
		tfOpenTime.setSize(Size.of(30, 12));
		tfOpenTime.setPosition(Position.rightAligned(5).y(y));
		tfOpenTime.register(this);
		UILabel lblOpenTime = new UILabel(this, "gui.door_factory.door_open_time");
		lblOpenTime.setPosition(Position.of(0, y + 2));
		propContainer.add(lblOpenTime);
		propContainer.add(tfOpenTime);

		//Auto close time
		y += 12;
		tfAutoCloseTime = new UITextField(this, null);
		tfAutoCloseTime.setSize(Size.of(30, 12));
		tfAutoCloseTime.setPosition(Position.rightAligned(5).y(y));
		tfAutoCloseTime.register(this);
		UILabel lblAutoCloseTime = new UILabel(this, "gui.door_factory.door_auto_close_time");
		lblAutoCloseTime.setPosition(Position.of(0, y + 2));
		propContainer.add(lblAutoCloseTime);
		propContainer.add(tfAutoCloseTime);

		//Double door
		y += 12;
		cbDoubleDoor = new UICheckBox(this);
		cbDoubleDoor.setPosition(Position.rightAligned(15).y(y));
		cbDoubleDoor.register(this);
		UILabel lblDoubleDoor = new UILabel(this, "gui.door_factory.door_double_door");
		lblDoubleDoor.setPosition(Position.of(0, y + 2));
		propContainer.add(lblDoubleDoor);
		propContainer.add(cbDoubleDoor);

		//Proximity detection
		y += 12;
		cbProximity = new UICheckBox(this);
		cbProximity.setPosition(Position.rightAligned(15).y(y));
		cbProximity.register(this);
		UILabel lblProximity = new UILabel(this, "gui.door_factory.proximity_detection");
		lblProximity.setPosition(Position.of(0, y + 2));
		propContainer.add(lblProximity);
		propContainer.add(cbProximity);

		//Redstone behavior
		y += 12;
		selRedstone = new UISelect<>(this, 100, Lists.newArrayList(RedstoneBehavior.values()));
		selRedstone.setPosition(Position.rightAligned().y(y));
		selRedstone.setLabelPattern("gui.door_factory.redstone_behavior.%s").register(this);
		UILabel lblRedstone = new UILabel(this, "gui.door_factory.redstone_behavior");
		lblRedstone.setPosition(Position.of(0, y + 2));
		propContainer.add(lblRedstone);
		propContainer.add(selRedstone);

		//Door sound
		y += 12;
		selDoorSound = new UISelect<>(this, 100, getSortedList(DoorRegistry.listSounds().keySet(), "gui.door_factory.door_sound."));
		selDoorSound.setPosition(Position.rightAligned().y(y));
		selDoorSound.setLabelPattern("gui.door_factory.door_sound.%s").register(this);
		UILabel lblDoorSound = new UILabel(this, "gui.door_factory.door_sound");
		lblDoorSound.setPosition(Position.of(0, y + 2));
		propContainer.add(lblDoorSound);
		propContainer.add(selDoorSound);

		return propContainer;
	}

	private ImmutableList<String> getSortedList(Set<String> set, String prefix)
	{
		return FluentIterable.from(set).toSortedList((String s1, String s2) -> {
			return I18n.format(prefix + s1).compareTo(I18n.format(prefix + s2));
		});
	}

	private UIContainer<?> getMaterialsContainer()
	{
		UIContainer<?> matContainer = new UIContainer<>(this, Size.relativeWidth(1.0F).height(80));
		matContainer.setPosition(Position.of(0, 15));

		rbCreate = new UIRadioButton(this, "rbDoor", "gui.door_factory.rb_create");
		rbCreate.setPosition(Position.of(30, 0));
		rbCreate.register(this);
		rbEdit = new UIRadioButton(this, "rbDoor", "gui.door_factory.rb_edit");
		rbEdit.setPosition(Position.of(100, 0));
		rbEdit.register(this);

		matContainer.add(rbCreate);
		matContainer.add(rbEdit);

		contCreate = new UIContainer<>(this);
		contCreate.setPosition(Position.of(0, 14));

		int y = 0;
		UISlot frameSlot = new UISlot(this, tileEntity.frameSlot);
		frameSlot.setPosition(Position.rightAligned(10).y(y));
		UISlot topMaterialSlot = new UISlot(this, tileEntity.topMaterialSlot);
		topMaterialSlot.setPosition(Position.rightAligned(10).y(y + 18));
		UISlot bottomMaterialSlot = new UISlot(this, tileEntity.bottomMaterialSlot);
		bottomMaterialSlot.setPosition(Position.rightAligned(10).y(y + 36));

		UILabel lblFrameType = new UILabel(this, "gui.door_factory.frame_type");
		lblFrameType.setPosition(Position.of(0, y + 5));
		contCreate.add(lblFrameType);
		UILabel lblTopMaterial = new UILabel(this, "gui.door_factory.top_material");
		lblTopMaterial.setPosition(Position.of(0, y + 23));
		contCreate.add(lblTopMaterial);
		UILabel lblBottomMaterial = new UILabel(this, "gui.door_factory.bottom_material");
		lblBottomMaterial.setPosition(Position.of(0, y + 41));
		contCreate.add(lblBottomMaterial);

		contCreate.add(frameSlot);
		contCreate.add(topMaterialSlot);
		contCreate.add(bottomMaterialSlot);

		contEdit = new UIContainer<>(this);
		contEdit.setPosition(Position.of(0, 14));

		UISlot doorEditSlotSlot = new UISlot(this, tileEntity.doorEditSlot);
		doorEditSlotSlot.setPosition(Position.rightAligned(10).y(18));
		UILabel lblDoorEditSlot = new UILabel(this, "gui.door_factory.door_edit_slot");
		lblDoorEditSlot.setPosition(Position.of(0, 23));
		contEdit.add(lblDoorEditSlot);
		contEdit.add(doorEditSlotSlot);

		matContainer.add(contCreate);
		matContainer.add(contEdit);

		return matContainer;
	}

	private UIContainer<?> getDigicodeContainer()
	{
		UIContainer<?> dcContainer = new UIContainer<>(this, Size.relativeWidth(1.0F).height(80));
		dcContainer.setPosition(Position.of(0, 15));

		digicode = new Digicode(this);
		digicode.setPosition(Position.centered().y(0));
		digicode.register(this);
		dcContainer.add(digicode);

		return dcContainer;
	}

	@Override
	public void updateGui()
	{
		boolean isCreate = tileEntity.isCreate();
		if (isCreate)
			rbCreate.setSelected();
		else
			rbEdit.setSelected();
		contCreate.setVisible(isCreate);
		contEdit.setVisible(!isCreate);
		btnCreate.setText(isCreate ? "gui.door_factory.create_door" : "gui.door_factory.edit_door");

		selDoorMovement.setSelectedOption(DoorRegistry.getId(tileEntity.getDoorMovement()));
		tfOpenTime.setText(Integer.toString(tileEntity.getOpeningTime()));
		tfAutoCloseTime.setText(Integer.toString(tileEntity.getAutoCloseTime()));
		cbDoubleDoor.setChecked(tileEntity.isDoubleDoor());
		cbProximity.setChecked(tileEntity.hasProximityDetection());
		selRedstone.select(tileEntity.getRedstoneBehavior());
		selDoorSound.setSelectedOption(DoorRegistry.getId(tileEntity.getDoorSound()));
	}

	@Subscribe
	public void onCheckedEvent(UICheckBox.CheckEvent event)
	{
		if (event.getComponent() == cbDoubleDoor)
			tileEntity.setDoubleDoor(event.isChecked());
		else if (event.getComponent() == cbProximity)
			tileEntity.setProximityDetection(event.isChecked());

		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onSelectEvent(UISelect.SelectEvent<?> event)
	{
		if (event.getNewValue() == null)
			return;

		if (event.getComponent() == selRedstone)
			tileEntity.setRedstoneBehavior((RedstoneBehavior) event.getNewValue());
		else if (event.getComponent() == selDoorMovement)
			tileEntity.setDoorMovement(DoorRegistry.getMovement((String) event.getNewValue()));
		else if (event.getComponent() == selDoorSound)
			tileEntity.setDoorSound(DoorRegistry.getSound((String) event.getNewValue()));

		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onRbSelectEvent(UIRadioButton.SelectEvent event)
	{
		boolean isCreate = event.getNewValue() == rbCreate;

		tileEntity.setCreate(isCreate);
		contCreate.setVisible(isCreate);
		contEdit.setVisible(!isCreate);
		btnCreate.setText(isCreate ? "gui.door_factory.create_door" : "gui.door_factory.edit_door");

		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onGuiChangeEvent(ValueChange<?, ?> event)
	{
		if (event.getComponent() != tfOpenTime && event.getComponent() != tfAutoCloseTime)
			return;

		try
		{
			int value = Integer.decode((String) event.getNewValue());

			if (event.getComponent() == tfOpenTime)
				tileEntity.setOpeningTime(value);
			if (event.getComponent() == tfAutoCloseTime)
				tileEntity.setAutoCloseTime(value);

			DoorFactoryMessage.sendDoorInformations(tileEntity);
		}
		catch (NumberFormatException e)
		{
			//parsing failed, replace the value of the textfield by the value already in the TE
			//tfOpenTime.setText(Integer.toString(tileEntity.getOpeningTime()));
		}
	}

	@Subscribe
	public void onDigicodeChange(Digicode.CodeChangeEvent event)
	{
		tileEntity.setCode(event.getCode());
		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onCreateDoor(UIButton.ClickEvent event)
	{
		DoorFactoryMessage.sendCreateDoor(tileEntity);
	}

	@Subscribe
	public void onTabActivation(ActiveStateChange<UITab> event)
	{
		if (event.getState())
		{
			activeTab = event.getComponent().getName();

			if ("tab_dc".equals(activeTab))
				registerKeyListener(digicode);
			else
				unregisterKeyListener(digicode);
		}
	}

}
