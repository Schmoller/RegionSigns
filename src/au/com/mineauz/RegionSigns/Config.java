package au.com.mineauz.RegionSigns;

import java.io.File;

import org.bukkit.configuration.InvalidConfigurationException;

public class Config extends AutoConfig
{

	public Config( File file )
	{
		super(file);
	}

	/// Claim Settings
	
	@ConfigField(name="max-price",category="claim",comment="The maximum price that you can put on a sign. Setting to 0 means no maximum price")
	public double maxClaimPrice = 0;
	@ConfigField(name="sign-format",category="claim",comment="The format of the sign after it has been claimed\nThere are 2 tags you can use in the text\n- <user> which is replaced with the players name\n- <region> which is replaced with the regions name")
	public String[] claimSign = {"<user>","","<region>",""};
	
	@ConfigField(name="initial-region-settings", category="claim", comment="Here you can create a list of settings to apply to a region that a claim sign is created for.\nThese should be region commands you wish to execute. For example: 'parent <parent>' or 'flag use deny'.\nExternal commands can be used and will be executed as though from a console. These must start with a slash.\nThere are some tags you can use as placeholders:\n- <parent> which is the name of the lowest region in the hierarchy at the location of the sign.\n- <creator> which is the name of the player creating the sign.")
	public String[] claimRegionSettings = {""};
	
	/// Rent Settings
	
	@ConfigField(name="check-interval",category="rent",comment="The number of ticks between checks for rent collection etc. Default is 1200 = 1 Minute")
	public int checkInterval = 1200;
	@ConfigField(name="autosave-interval", category="rent",comment="The autosave frequency in ticks for the rent system. Default is 6000 = 5 Minutes")
	public int autosaveInterval = 6000;
	
	@ConfigField(name="max-payment-interval", category="rent",comment="The maximum per interval payment that can be requested. Setting to 0 means no maximum price")
	public double maxRentIntervalPayment = 0;
	@ConfigField(name="max-payment-upfront", category="rent",comment="The maximum upfront payment that can be requested. Setting to 0 means no maximum price")
	public double maxRentUpfrontPayment = 0;
	
	@ConfigField(name="default-period",category="rent",comment="The default payment interval in standard date-diff format")
	private String mDefaultRentPeriod = "1w";
	public long defaultRentPeriod;
	
	@ConfigField(name="minimum-period",category="rent",comment="The minimum time somebody can rent for in standard date-diff format. Set to 0 for no minimum period")
	private String mMinimumRentPeriod = "2w";
	public long minimumRentPeriod;
	
	@ConfigField(name="sign-format",category="rent",comment="The format of the sign after it has been rented\nThere are 2 tags you can use in the text\n- <user> which is replaced with the players name\n- <region> which is replaced with the regions name")
	public String[] rentSign = {"<user>","","<region>",""};
	
	@ConfigField(name="initial-region-settings", category="rent", comment="Here you can create a list of settings to apply to a region that a rent sign is created for.\nThese should be region commands you wish to execute. For example: 'parent <parent>' or 'flag use deny'.\nExternal commands can be used and will be executed as though from a console. These must start with a slash.\nThere are some tags you can use as placeholders:\n- <parent> which is the name of the lowest region in the hierarchy at the location of the sign.\n- <creator> which is the name of the player creating the sign.\n- <owner> the owner of the region. (in the case where there are multiple owners, commands using this will be called for each owner)")
	public String[] rentRegionSettings = {""};
	
	/// Sale signs
	@ConfigField(name="max-price",category="sale",comment="The maximum price that you can put on a sale sign. Setting to 0 means no maximum price")
	public double maxSalePrice = 0;
	
	/// Management Signs
	@ConfigField(name="unclaimed-sign-format",category="general",comment="The format of the sign after the region has been unclaimed.\nYou can use the <region> tag to indicate the region name")
	public String[] unclaimedSign = {"<region>","Unclaimed","Waiting for","reset"};
	@ConfigField(name="evicted-sign-format",category="general",comment="The format of the sign after the tenant of a region has been evicted.\nYou can use the <region> tag to indicate the region name, <user> which is replaced with the tenants name.")
	public String[] evictedSign = {"<user>","has been","evicted.","<region>"};
	
	/// Messages
	@ConfigField(name="prefix",category="rent.messages", comment="This is the prefix of all messages")
	public String messagePrefix = "&a[Rent]&f ";
	
	@ConfigField(name="rent-begin",category="rent.messages", comment="This message is shown when a player begins renting a region")
	public String messageBeginRenting = "You have started renting &e'<region>'&f. &c<payment>&f was spent.";
	@ConfigField(name="rent-begin-free",category="rent.messages", comment="This message is shown when a player begins renting a region that doesnt cost anything to start")
	public String messageBeginRentingFree = "You have started renting &e'<region>'&f.";
	@ConfigField(name="rent-tenant-begin",category="rent.messages", comment="This message is shown to the landlord when a player begins renting a region")
	public String messageTenantBegin = "Your region &e'<region>'&f has been rented.";
	
	@ConfigField(name="payment-first",category="rent.messages", comment="This message is shown after you have begun renting a region")
	public String messageFirstPayment = "The first payment of &c<payment>&f will be deducted in &e<time>&f.";
	@ConfigField(name="payment-normal",category="rent.messages", comment="This message is shown when a player sends payment for rent")
	public String messagePayment = "&c<payment>&f has been paid for &e'<region>'&f.";
	@ConfigField(name="payment-received",category="rent.messages", comment="This message is shown when a player receives payment for rent")
	public String messagePaymentReceived = "&a<payment>&f has been received from the tenant of &e'<region>'&f.";
	@ConfigField(name="payment-next",category="rent.messages", comment="This message is shown after payment of rent, showing the next time of rent collection")
	public String messageNextPayment = "The next payment will be deducted in &e<time>&f.";
	
	@ConfigField(name="rent-completed",category="rent.messages", comment="This message is shown when a player finishes renting a region")
	public String messageFinished = "You have finished renting &e'<region>'&f. Please contact an Admin or Mod to retrieve any remaining possessions.";
	@ConfigField(name="rent-tenant-completed",category="rent.messages", comment="This message is shown to the landlord when a player finishes renting a region")
	public String messageTenantFinished = "Your tenant has finished renting &e'<region>'&f.";
	@ConfigField(name="rent-stop",category="rent.messages", comment="This message is shown when a player terminates their, or someone elses, lease")
	public String messageTerminate = "You have stopped renting &e'<region>'&f. A final payment of &c<payment>&f was sent. You have &e<time>&f to move out.";
	@ConfigField(name="rent-tenant-stop",category="rent.messages", comment="This message is shown when a player terminates their, or someone elses, lease")
	public String messageTenantTerminate = "Your tenant has decided to stop renting &e'<region>'&f. They have &e<time>&f to move out.";
	@ConfigField(name="rent-evicted",category="rent.messages", comment="This message is shown if a player is evicted from a region")
	public String messageEvicted = "&cYou have been evicted from &e'<region>'.&c Please contact an Admin or Mod to retrieve any remaining possessions.";
	@ConfigField(name="rent-tenant-evicted",category="rent.messages", comment="This message is shown to the landlord if a player is evicted from a region")
	public String messageTenantEvicted = "&cYour tenant has been evicted from &e'<region>'.&c";
	
	@ConfigField(name="warning-funds",category="rent.messages", comment="This message is shown after payment of rent, if a player does not have enough money for the next payment")
	public String messageWarningFunds = "&6Warning: You currently have insufficient funds to pay your next rent";
	@ConfigField(name="warning-eviction",category="rent.messages", comment="This message is shown if a player is unable to pay rent")
	public String messageWarningEvict = "&6Warning: You will be evicted from &e'<region>'&6 if payment is not received in &e<time>&6.";
	
	@Override
	protected void onPostLoad() throws InvalidConfigurationException
	{
		defaultRentPeriod = Util.parseDateDiff(mDefaultRentPeriod);
		minimumRentPeriod = Util.parseDateDiff(mMinimumRentPeriod);
	}
	
	@Override
	protected void onPreSave()
	{
		mDefaultRentPeriod = Util.formatDateDiffShort(defaultRentPeriod);
		mMinimumRentPeriod = Util.formatDateDiffShort(minimumRentPeriod);
	}
}
