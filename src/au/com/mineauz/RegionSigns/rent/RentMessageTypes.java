package au.com.mineauz.RegionSigns.rent;

public enum RentMessageTypes
{
	// When the player cannot afford the next payment
	InsufficientFunds,
	// When the player failed to pay rent
	EvictionPending,
	// When the player has been evicted
	Eviction,
	// When the player has been evicted, on the landlords side
	EvictionLandlord,
	// When the player has asked to stop renting
	RentEnding,
	// When the player has asked to stop renting, on the landlords side
	RentEndingLandlord,
	// When the renting has actually ended
	RentEnded,
	// When the renting has actually ended, on the landlords side
	RentEndedLandlord,
	// When a payment for rent has been sent
	PaymentSent,
	// The time of the next payment
	NextPaymentTime,
	// When the player stars renting a region
	RentBegin,
	// When the player stars renting a region, on the landlords side
	RentBeginLandlord,
	// When the player stars renting a region that doesnt cost anything to start
	RentBeginFree,
	// The time of the first payment
	FirstPaymentTime,
	// When a payment for rent has been received
	PaymentReceived
}
