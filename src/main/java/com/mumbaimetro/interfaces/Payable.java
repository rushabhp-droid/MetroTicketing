package com.mumbaimetro.interfaces;

import com.mumbaimetro.exceptions.InsufficientBalanceException;

/**
 * Interface representing any entity capable of financial transactions
 * Demonstrates: Interface, Abstract Methods, Exception Declaration in Interface
 */
public interface Payable {
    /**
     * Recharges the balance with the specified amount
     * @param amount positive currency value to add
     */
    void recharge(double amount);

    /**
     * Deducts the specified fare amount
     * @param amount fare to deduct
     * @throws InsufficientBalanceException when current balance is less than amount
     */
    void deduct(double amount) throws InsufficientBalanceException;

    /**
     * Retrieves the current balance
     * @return current balance
     */
    double getBalance();
}
