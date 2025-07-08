package com.proptech.realestate.model.enums;

/**
 * RESO Data Dictionary 2.0 - StandardStatus Enum.
 * The status of the listing as it reflects the state of the contract between
 * the listing agent and seller or an agreement with a buyer.
 * 
 * @see <a href="https://ddwiki.reso.org/display/DDW17/StandardStatus+Field">RESO DD Wiki: StandardStatus</a>
 */
public enum StandardStatus {
    /**
     * An active listing. The listing is available for sale and for showing.
     */
    Active,

    /**
     * The listing is under contract and the seller is not entertaining other offers.
     * Often referred to as "Pending."
     */
    Pending,

    /**
     * The listing is under contract, but the seller is still entertaining other offers.
     * Sometimes referred to as "Active Under Contract," "Contingent," or "Backup."
     */
    ActiveUnderContract,

    /**
     * A listing that has been sold and the transaction has been completed.
     */
    Closed,

    /**
     * A listing that has been leased.
     */
    Leased,
    
    /**
     * A listing contract that has been canceled between the seller and the listing broker.
     */
    Canceled,

    /**
     * The listing contract has expired.
     */
    Expired,

    /**
     * A listing that is temporarily not available for showing, but is still under a listing contract.
     * Sometimes referred to as "Temporarily Off Market" (TOM) or "Hold."
     */
    Hold,

    /**
     * A listing that has been withdrawn from the market. The listing contract is still in effect,
     * but the property is not available for sale.
     */
    Withdrawn,

    /**
     * A listing that is not yet active on the market. It might be in preparation or waiting
     * for a specific go-live date.
     * Sometimes referred to as "Coming Soon."
     */
    ComingSoon,
    
    /**
     * A listing that was withdrawn and is now back on the market.
     * This is often a local status that maps to Active.
     */
    BackOnMarket,
    
    /**
     * A listing that was under contract but the deal fell through, and is now active again.
     * This is often a local status that maps to Active.
     */
    ActiveContingent,

    /**
     * The status of the listing is unknown or not provided.
     */
    Unknown;

    /**
     * Checks if the listing is considered "On Market".
     * On-market statuses are those where the property is available for offers.
     */
    public boolean isOnMarket() {
        return this == Active || this == ActiveUnderContract || this == ComingSoon || this == ActiveContingent;
    }

    /**
     * Checks if the listing is considered "Off Market".
     * Off-market statuses are those where the property is no longer available for new offers.
     */
    public boolean isOffMarket() {
        return this == Pending || this == Closed || this == Leased || this == Canceled || this == Expired || this == Withdrawn;
    }

    /**
     * Checks if the listing is under a binding contract.
     */
    public boolean isUnderContract() {
        return this == Pending || this == ActiveUnderContract || this == ActiveContingent;
    }

    /**
     * Checks if the transaction has been finalized (sold or leased).
     */
    public boolean isFinalized() {
        return this == Closed || this == Leased;
    }
} 