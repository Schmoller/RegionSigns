package com.gmail.steven.schmoll.RegionSigns;

public class RentMessage
{
	/// The type of message to show
	public RentMessageTypes Type;
	/// The time that the event will happen.
	public long EventCompletionTime;
	public String Region;
	/// The amount of payment if used.
	public double Payment;
}

enum RentMessageTypes
{
	// When the player cannot afford the next payment
	InsufficientFunds,
	// When the player failed to pay rent
	EvictionPending,
	// When the player has been evicted
	Eviction,
	// When the player has asked to stop renting
	RentEnding,
	// When the renting has actually ended
	RentEnded,
	// When a payment for rent has been sent
	PaymentSent,
	// The time of the next payment
	NextPaymentTime,
	// When the player stars renting a region
	RentBegin,
	// When the player stars renting a region that doesnt cost anything to start
	RentBeginFree,
	// The time of the first payment
	FirstPaymentTime,
}
