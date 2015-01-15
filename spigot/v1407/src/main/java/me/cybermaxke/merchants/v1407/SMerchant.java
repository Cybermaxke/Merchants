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
package me.cybermaxke.merchants.v1407;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.server.v1_4_R1.Container;
import net.minecraft.server.v1_4_R1.EntityHuman;
import net.minecraft.server.v1_4_R1.EntityPlayer;
import net.minecraft.server.v1_4_R1.IMerchant;
import net.minecraft.server.v1_4_R1.MerchantRecipe;
import net.minecraft.server.v1_4_R1.MerchantRecipeList;
import net.minecraft.server.v1_4_R1.Packet100OpenWindow;
import net.minecraft.server.v1_4_R1.Packet250CustomPayload;

import org.bukkit.craftbukkit.v1_4_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_R1.event.CraftEventFactory;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import me.cybermaxke.merchants.api.Merchant;
import me.cybermaxke.merchants.api.MerchantOffer;

@SuppressWarnings("unchecked")
public class SMerchant implements IMerchant, Merchant {
	// The recipes list
	private final MerchantRecipeList offers = new MerchantRecipeList();

	// The customers
	private final Set<Player> customers = Sets.newHashSet();

	// The title of the merchant
	private final String title;

	public SMerchant(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	@Override
	public void removeOffer(MerchantOffer offer) {
		this.offers.remove(offer);

		// Link the offer
		((SMerchantOffer) offer).remove(this);

		// Send the new offer list
		this.sendUpdate();
	}

	@Override
	public void removeOffers(Iterable<MerchantOffer> offers) {
		this.offers.removeAll((Lists.newArrayList(offers)));

		// Link the offers
		for (MerchantOffer offer : offers) {
			((SMerchantOffer) offer).remove(this);
		}

		// Send the new offer list
		this.sendUpdate();
	}

	@Override
	public void addOffer(MerchantOffer offer) {
		this.offers.add(offer);

		// Link the offer
		((SMerchantOffer) offer).add(this);

		// Send the new offer list
		this.sendUpdate();
	}

	@Override
	public void addOffers(Iterable<MerchantOffer> offers) {
		this.offers.addAll(Lists.newArrayList(offers));

		// Link the offers
		for (MerchantOffer offer : offers) {
			((SMerchantOffer) offer).add(this);
		}

		// Send the new offer list
		this.sendUpdate();
	}

	@Override
	public void sortOffers(Comparator<MerchantOffer> comparator) {
		Collections.sort(this.offers, comparator);

		// Send the new offer list
		this.sendUpdate();
	}

	@Override
	public List<MerchantOffer> getOffers() {
		return Lists.newArrayList(this.offers);
	}

	@Override
	public boolean addCustomer(Player player) {
		if (this.customers.add(player)) {
			EntityPlayer player0 = ((CraftPlayer) player).getHandle();
			Container container0 = null;

			try {
				container0 = new SContainerMerchant(player0, this);
				container0 = CraftEventFactory.callInventoryOpenEvent(player0, container0);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (container0 == null) {
				this.customers.remove(player);
				return false;
			}

			int window = player0.nextContainerCounter();

			player0.activeContainer = container0;
			player0.activeContainer.windowId = window;
			player0.activeContainer.addSlotListener(player0);

			// Open the window
			player0.playerConnection.sendPacket(new Packet100OpenWindow(window, 6, this.title, 3));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			try {
				// Write the window id
				dos.writeInt(window);
				// Write the offers
				this.offers.a(dos);
				// Flush and close data stream
				dos.flush();
				dos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Send the offers
			player0.playerConnection.sendPacket(new Packet250CustomPayload("MC|TrList", baos.toByteArray()));
			return true;
		}

		return false;
	}

	@Override
	public boolean removeCustomer(Player player) {
		if (this.customers.remove(player)) {
			player.closeInventory();
			return true;
		}

		return false;
	}

	@Override
	public boolean hasCustomer(Player player) {
		return this.customers.contains(player);
	}

	@Override
	public Collection<Player> getCustomers() {
		return Lists.newArrayList(this.customers);
	}

	@Override
	public MerchantRecipeList getOffers(EntityHuman human) {
		return this.offers;
	}

	@Override
	public void a(MerchantRecipe arg0) {
		// Not used
	}

	@Override
	public void b_(EntityHuman arg0) {
		// Not used
	}

	@Override
	public EntityHuman m_() {
		// Not used
		return null;
	}

	// Called when the merchant requires a update
	void sendUpdate() {
		if (this.customers.isEmpty()) {
			return;
		}

		ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
		DataOutputStream dos0 = new DataOutputStream(baos0);

		// Write the recipe list
		this.offers.a(dos0);

		try {
			dos0.flush();
			dos0.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Get the bytes
		byte[] data = baos0.toByteArray();

		// Send a packet to all the players
		Iterator<Player> it = this.customers.iterator();
		while (it.hasNext()) {
			EntityPlayer player0 = ((CraftPlayer) it.next()).getHandle();

			// Every player has a different window id
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			DataOutputStream dos1 = new DataOutputStream(baos1);

			try {
				dos1.writeInt(player0.activeContainer.windowId);
				dos1.write(data);
				dos1.flush();
				dos1.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			player0.playerConnection.sendPacket(new Packet250CustomPayload("MC|TrList", baos1.toByteArray()));
		}
	}

}