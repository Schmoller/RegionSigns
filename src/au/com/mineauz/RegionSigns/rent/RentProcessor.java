package au.com.mineauz.RegionSigns.rent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import au.com.mineauz.RegionSigns.RegionSigns;
import au.com.mineauz.RegionSigns.Util;

public class RentProcessor implements Runnable
{

	@Override
	public void run()
	{
		RentStatus rent = RentManager.instance.popNextExpired();
		while (rent != null)
		{
			processRent(rent);
			rent = RentManager.instance.popNextExpired();
		}
	}

	private void processRent(RentStatus rent)
	{
		if(rent.PendingRemoval)
			processEndRent(rent, false);
		
		// Handle payments
		else if(rent.IntervalPayment > 0 && rent.RentInterval > 0)
		{
			if(Util.playerSubtractMoney(rent.Tenant, rent.IntervalPayment))
			{
				RegionSigns.instance.getLogger().info(String.format("Taking $%.2f from %s to pay rent for %s leaving them with $%.2f", rent.IntervalPayment, rent.Tenant.getName(), rent.Region, Util.getPlayerMoney(rent.Tenant)));
				// The message about a payment being made
				RentMessage paymentMade = new RentMessage();
				paymentMade.Type = RentMessageTypes.PaymentSent;
				paymentMade.EventCompletionTime = 0;
				paymentMade.Region = rent.Region;
				paymentMade.Payment = rent.IntervalPayment;
				
				// The message about the next payment time
				RentMessage paymentTime = new RentMessage();
				paymentTime.Type = RentMessageTypes.NextPaymentTime;
				paymentTime.EventCompletionTime = (rent.PendingEviction ? rent.NextIntervalEnd : rent.NextIntervalEnd + rent.RentInterval);
				paymentTime.Region = rent.Region;
				paymentTime.Payment = rent.IntervalPayment;
				
				RentManager.instance.sendMessage(paymentMade, rent.Tenant);
				RentManager.instance.sendMessage(paymentTime, rent.Tenant);
				
				rent.NextIntervalEnd = rent.NextIntervalEnd + rent.RentInterval;
				RentManager.instance.pushRent(rent, rent.NextIntervalEnd);
				
				if(!Util.playerHasEnough(rent.Tenant, rent.IntervalPayment))
				{
					// The message about not having enough for next time
					RentMessage fundsWarning = new RentMessage();
					fundsWarning.Type = RentMessageTypes.InsufficientFunds;
					fundsWarning.EventCompletionTime = 0;
					fundsWarning.Region = rent.Region;
					fundsWarning.Payment = rent.IntervalPayment;
					
					RentManager.instance.sendMessage(fundsWarning, rent.Tenant);
				}
			}
			else
			{
				if(rent.PendingEviction)
					processEndRent(rent,true);
				else
				{
					rent.PendingEviction = true;
					
					RentMessage evictWarning = new RentMessage();
					evictWarning.Type = RentMessageTypes.EvictionPending;
					evictWarning.Region = rent.Region;
					evictWarning.EventCompletionTime = rent.NextIntervalEnd + rent.RentInterval / 2;
					evictWarning.Payment = rent.IntervalPayment;
					
					// Notify the tenant
					RentManager.instance.sendMessage(evictWarning,rent.Tenant);
					
					rent.NextIntervalEnd = rent.NextIntervalEnd + rent.RentInterval;
					
					RentManager.instance.pushRent(rent, rent.NextIntervalEnd - rent.RentInterval / 2);
				}
			}
		}
		else
		{
			if(rent.RentInterval != 0)
				RentManager.instance.pushRent(rent, rent.NextIntervalEnd + rent.RentInterval);
			else
				RentManager.instance.pushRent(rent, rent.NextIntervalEnd + Util.parseDateDiff("1w"));
		}
	}
	
	private void processEndRent(RentStatus rent, boolean evict)
	{
		ProtectedRegion region = Util.getRegion(Bukkit.getWorld(rent.World), rent.Region);
		
		if(region != null)
		{
			// Remove the tenant(s)
			region.setMembers(new DefaultDomain());
			
			Util.saveRegionManager(Bukkit.getWorld(rent.World));
		}
		
		RentMessage finishedMsg = new RentMessage();
		finishedMsg.Type = (evict ? RentMessageTypes.Eviction : RentMessageTypes.RentEnded);
		finishedMsg.Region = rent.Region;
		finishedMsg.EventCompletionTime = 0;
		finishedMsg.Payment = (evict ? rent.IntervalPayment : 0);
		
		// Notify the tenant
		RentManager.instance.sendMessage(finishedMsg,rent.Tenant);
		
		// Update the sign
		if(rent.SignLocation != null && (rent.SignLocation.getBlock().getType() == Material.WALL_SIGN || rent.SignLocation.getBlock().getType() == Material.SIGN_POST))
		{
			Sign sign = (Sign)rent.SignLocation.getBlock().getState();
			
			if(evict)
			{
				for(int i = 0; i < 4; ++i)
				{
					String line = RegionSigns.config.evictedSign[i];
					line = line.replaceAll("<region>", rent.Region);
					line = line.replaceAll("<user>", rent.Tenant.getName());
					
					sign.setLine(i, line);
				}
			}
			else
			{
				for(int i = 0; i < 4; ++i)
				{
					String line = RegionSigns.config.unclaimedSign[i];
					line = line.replaceAll("<region>", rent.Region);
					
					sign.setLine(i, line);
				}
			}
			
			sign.update(true);
		}
	}
}
