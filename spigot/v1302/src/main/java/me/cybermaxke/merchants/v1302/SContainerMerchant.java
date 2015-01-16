/**
 * This file is part of MerchantsAPI.
 * 
 * Copyright (c) 2014, Cybermaxke
 * 
 * MerchantsAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MerchantsAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MerchantsAPI. If not, see <http://www.gnu.org/licenses/>.
 */
package me.cybermaxke.merchants.v1302;

import java.lang.reflect.Field;

import net.minecraft.server.ContainerMerchant;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryMerchant;
import net.minecraft.server.Slot;

import org.bukkit.entity.Player;

public class SContainerMerchant extends ContainerMerchant {
	// The field of the merchant inventory in the container
	private static Field fieldInventoryMerchant;

	// The merchant we are trading with
	private final SMerchant merchant;

	public SContainerMerchant(EntityPlayer customer, SMerchant merchant) throws Exception {
		super(customer.inventory, merchant, customer.world);
		this.merchant = merchant;

		// Search the field
		if (fieldInventoryMerchant == null) {
			for (Field field : ContainerMerchant.class.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.getType().isAssignableFrom(InventoryMerchant.class)) {
					fieldInventoryMerchant = field;
				}
			}
		}

		SInventoryMerchant inventory = new SInventoryMerchant(customer, merchant);

		this.setSlot(0, new Slot(inventory, 0, 36, 53));
		this.setSlot(1, new Slot(inventory, 1, 62, 53));
		this.setSlot(2, new SSlotMerchantResult(customer, merchant, inventory, 2, 120, 53));

		fieldInventoryMerchant.setAccessible(true);
		fieldInventoryMerchant.set(this, inventory);
	}

	/**
	 * Sets the slot at the raw index.
	 * 
	 * @param index the index
	 * @param slot the slot
	 */
	@SuppressWarnings("unchecked")
	void setSlot(int index, Slot slot) {
		// Set the raw index of the slot
		slot.d = index;

		// Override the slot at the index
		this.b.set(index, slot);
	}

	@Override
	public boolean c(EntityHuman human) {
		return this.merchant.hasCustomer((Player) human.getBukkitEntity());
	}

	@Override
	public void a(EntityHuman human) {
		super.a(human);

		// Called when the human is finished
		this.merchant.removeCustomer((Player) human.getBukkitEntity());
	}
}