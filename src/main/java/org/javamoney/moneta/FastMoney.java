/**
 * Copyright (c) 2012, 2014, Credit Suisse (Anatole Tresch), Werner Keil and others by the @author tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.javamoney.moneta;

import org.javamoney.moneta.internal.FastMoneyAmountFactory;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.javamoney.moneta.spi.MoneyUtils;

import javax.money.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>long</code> based implementation of {@link MonetaryAmount}.This class internally uses a
 * single long number as numeric representation, which basically is interpreted as minor units.<p>
 * It suggested to have a performance advantage of a 10-15 times faster compared to {@link Money},
 * which internally uses {@link BigDecimal}. Nevertheless this comes with a price of less precision.
 * As an example performing the following calculation one million times, results in slightly
 * different results:
 * </p>
 * <pre><code>
 * Money money1 = money1.add(Money.of(EURO, 1234567.3444));
 * money1 = money1.subtract(Money.of(EURO, 232323));
 * money1 = money1.multiply(3.4);
 * money1 = money1.divide(5.456);
 * </code></pre>
 * <p>
 * Executed one million (1000000) times this results in {@code EUR 1657407.962529182}, calculated in
 * 3680 ms, or roughly 3ns/loop.
 * <p>
 * whereas
 * </p>
 * <pre><code>
 * FastMoney money1 = money1.add(FastMoney.of(EURO, 1234567.3444));
 * money1 = money1.subtract(FastMoney.of(EURO, 232323));
 * money1 = money1.multiply(3.4);
 * money1 = money1.divide(5.456);
 * </code></pre>
 * <p>
 * executed one million (1000000) times results in {@code EUR 1657407.96251}, calculated in 179 ms,
 * which is less than 1ns/loop.
 * </p><p>
 * Also note than mixing up types my drastically change the performance behavior. E.g. replacing the
 * code above with the following: *
 * </p>
 * <pre><code>
 * FastMoney money1 = money1.add(Money.of(EURO, 1234567.3444));
 * money1 = money1.subtract(FastMoney.of(EURO, 232323));
 * money1 = money1.multiply(3.4);
 * money1 = money1.divide(5.456);
 * </code></pre>
 * <p>
 * executed one million (1000000) times may execute significantly longer, since monetary amount type
 * conversion is involved.
 * </p><p>
 * Basically, when mixing amount implementations, the performance of the amount, on which most of
 * the operations are operated, has the most significant impact on the overall performance behavior.
 *
 * @author Anatole Tresch
 * @author Werner Keil
 * @version 0.5.2
 */
public final class FastMoney implements MonetaryAmount, Comparable<MonetaryAmount>, Serializable{

    private static final long serialVersionUID = 1L;

    /**
     * The currency of this amount.
     */
    private final CurrencyUnit currency;

    /**
     * The numeric part of this amount.
     */
    private final long number;

    /**
     * The current scale represented by the number.
     */
    private static final int SCALE = 5;

    /**
     * the {@link MonetaryContext} used by this instance, e.g. on division.
     */
    private static final MonetaryContext MONETARY_CONTEXT =
            MonetaryContextBuilder.create(FastMoney.class).setMaxScale(SCALE).setFixedScale(true).setPrecision(19)
                    .build();
    /**
     * Default rounding context used.
     */
    //    private static final MathContext CALC_CONTEXT = new MathContext(19, RoundingMode.HALF_EVEN);

    /**
     * Maximum possible value supported, using XX (no currency).
     */
    public static final FastMoney MAX_VALUE = new FastMoney(Long.MAX_VALUE, MonetaryCurrencies.getCurrency("XXX"));
    /**
     * Maximum possible numeric value supported.
     */
    private static final BigDecimal MAX_BD = MAX_VALUE.getBigDecimal();
    /**
     * Minimum possible value supported, using XX (no currency).
     */
    public static final FastMoney MIN_VALUE = new FastMoney(Long.MIN_VALUE, MonetaryCurrencies.getCurrency("XXX"));
    /**
     * Minimum possible numeric value supported.
     */
    private static final BigDecimal MIN_BD = MIN_VALUE.getBigDecimal();


    /**
     * Creates a new instance os {@link FastMoney}.
     *
     * @param currency the currency, not null.
     * @param number   the amount, not null.
     */
    private FastMoney(Number number, CurrencyUnit currency, boolean allowInternalRounding){
        Objects.requireNonNull(currency, "Currency is required.");
        this.currency = currency;
        Objects.requireNonNull(number, "Number is required.");
        this.number = getInternalNumber(number, allowInternalRounding);
    }

    /**
     * Creates a new instance os {@link FastMoney}.
     *
     * @param currency    the currency, not null.
     * @param numberValue the numeric value, not null.
     */
    private FastMoney(NumberValue numberValue, CurrencyUnit currency, boolean allowInternalRounding){
        Objects.requireNonNull(currency, "Currency is required.");
        this.currency = currency;
        Objects.requireNonNull(numberValue, "Number is required.");
        this.number = getInternalNumber(numberValue.numberValue(BigDecimal.class), allowInternalRounding);
    }

    /**
     * Creates a new instance os {@link FastMoney}.
     *
     * @param number   The internal number value
     * @param currency the currency, not null.
     */
    private FastMoney(long number, CurrencyUnit currency){
        Objects.requireNonNull(currency, "Currency is required.");
        this.currency = currency;
        this.number = number;
    }

    /**
     * Returns the amount’s currency, modelled as {@link CurrencyUnit}.
     * Implementations may co-variantly change the return type to a more
     * specific implementation of {@link CurrencyUnit} if desired.
     *
     * @return the currency, never {@code null}
     * @see javax.money.MonetaryAmount#getCurrency()
     */
    @Override
    public CurrencyUnit getCurrency(){
        return currency;
    }

    /**
     * Access the {@link MonetaryContext} used by this instance.
     *
     * @return the {@link MonetaryContext} used, never null.
     * @see javax.money.MonetaryAmount#getMonetaryContext()
     */
    @Override
    public MonetaryContext getMonetaryContext(){
        return MONETARY_CONTEXT;
    }

    private long getInternalNumber(Number number, boolean allowInternalRounding){
        BigDecimal bd = MoneyUtils.getBigDecimal(number);
        if(!allowInternalRounding && bd.scale() > SCALE){
            throw new ArithmeticException(number + " can not be represented by this class, scale > " + SCALE);
        }
        if(bd.compareTo(MIN_BD) < 0){
            throw new ArithmeticException("Overflow: " + number + " < " + MIN_BD);
        }else if(bd.compareTo(MAX_BD) > 0){
            throw new ArithmeticException("Overflow: " + number + " > " + MAX_BD);
        }
        return bd.movePointRight(SCALE).longValue();
    }


    /**
     * Static factory method for creating a new instance of {@link FastMoney}.
     *
     * @param currency      The target currency, not null.
     * @param numberBinding The numeric part, not null.
     * @return A new instance of {@link FastMoney}.
     */
    public static FastMoney of(NumberValue numberBinding, CurrencyUnit currency){
        return new FastMoney(numberBinding, currency, false);
    }

    /**
     * Static factory method for creating a new instance of {@link FastMoney}.
     *
     * @param currency The target currency, not null.
     * @param number   The numeric part, not null.
     * @return A new instance of {@link FastMoney}.
     */
    public static FastMoney of(Number number, CurrencyUnit currency){
        return new FastMoney(number, currency, false);
    }

    /**
     * Static factory method for creating a new instance of {@link FastMoney}.
     *
     * @param currencyCode The target currency as currency code.
     * @param number       The numeric part, not null.
     * @return A new instance of {@link FastMoney}.
     */
    public static FastMoney of(Number number, String currencyCode){
        CurrencyUnit currency = MonetaryCurrencies.getCurrency(currencyCode);
        return of(number, currency);
    }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MonetaryAmount o){
        Objects.requireNonNull(o);
        int compare = getCurrency().getCurrencyCode().compareTo(o.getCurrency().getCurrencyCode());
        if(compare == 0){
            compare = getNumber().numberValue(BigDecimal.class).compareTo(o.getNumber().numberValue(BigDecimal.class));
        }
        return compare;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode(){
        return Objects.hash(currency, number);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj){
        if(obj == this){
            return true;
        }
        if(obj instanceof FastMoney){
            FastMoney other = (FastMoney) obj;
            return Objects.equals(currency, other.currency) && Objects.equals(number, other.number);
        }
        return false;
    }


    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#abs()
     */
    public FastMoney abs(){
        if(this.isPositiveOrZero()){
            return this;
        }
        return this.negate();
    }

    // Arithmetic Operations

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#add(javax.money.MonetaryAmount)
     */
    public FastMoney add(MonetaryAmount amount){
        checkAmountParameter(amount);
        if(amount.isZero()){
            return this;
        }
        return new FastMoney(Math.addExact(this.number, getInternalNumber(amount.getNumber(), false)), getCurrency());
    }

    private void checkAmountParameter(MonetaryAmount amount){
        MoneyUtils.checkAmountParameter(amount, this.currency);
        // numeric check for overflow...
        if(amount.getNumber().getScale() > SCALE){
            throw new ArithmeticException("Parameter exceeds maximal scale: " + SCALE);
        }
        if(amount.getNumber().getPrecision() > MAX_BD.precision()){
            throw new ArithmeticException("Parameter exceeds maximal precision: " + SCALE);
        }
    }


    /*
         * (non-Javadoc)
         * @see javax.money.MonetaryAmount#divide(java.lang.Number)
         */
    public FastMoney divide(Number divisor){
        checkNumber(divisor);
        if(isOne(divisor)){
            return this;
        }
        return new FastMoney(Math.round(this.number / divisor.doubleValue()), getCurrency());
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#divideAndRemainder(java.lang.Number)
     */
    public FastMoney[] divideAndRemainder(Number divisor){
        checkNumber(divisor);
        if(isOne(divisor)){
            return new FastMoney[]{this, FastMoney.of(0, getCurrency())};
        }
        BigDecimal div = MoneyUtils.getBigDecimal(divisor);
        BigDecimal[] res = getBigDecimal().divideAndRemainder(div);
        return new FastMoney[]{new FastMoney(res[0], getCurrency(), true), new FastMoney(res[1], getCurrency(), true)};
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#divideToIntegralValue(java.lang.Number)
     */
    public FastMoney divideToIntegralValue(Number divisor){
        checkNumber(divisor);
        if(isOne(divisor)){
            return this;
        }
        BigDecimal div = MoneyUtils.getBigDecimal(divisor);
        return new FastMoney(getBigDecimal().divideToIntegralValue(div), getCurrency(), false);
    }

    public FastMoney multiply(Number multiplicand){
        checkNumber(multiplicand);
        if(isOne(multiplicand)){
            return this;
        }
        return new FastMoney(Math.round(this.number * multiplicand.doubleValue()), getCurrency());
        //        BigDecimal mult = MoneyUtils.getBigDecimal(multiplicand);
        //            return new FastMoney(mult.multiply(BigDecimal.valueOf(this.number),
        // CALC_CONTEXT).longValueExact(), getCurrency());
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#negate()
     */
    public FastMoney negate(){
        return new FastMoney(this.number * -1, getCurrency());
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#plus()
     */
    public FastMoney plus(){
        if(this.number >= 0){
            return this;
        }
        return new FastMoney(this.number * -1, getCurrency());
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#subtract(javax.money.MonetaryAmount)
     */
    public FastMoney subtract(MonetaryAmount subtrahend){
        checkAmountParameter(subtrahend);
        if(subtrahend.isZero()){
            return this;
        }
        long subtrahendAsLong = getInternalNumber(subtrahend.getNumber(), false);
        return new FastMoney(Math.addExact(this.number, -1L * subtrahendAsLong), getCurrency());
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#remainder(java.lang.Number)
     */
    public FastMoney remainder(Number divisor){
        checkNumber(divisor);
        if(isOne(divisor)){
            return new FastMoney(0, getCurrency());
        }
        return new FastMoney(this.number % getInternalNumber(divisor, true), getCurrency());
    }

    private boolean isOne(Number number){
        BigDecimal bd = MoneyUtils.getBigDecimal(number);
        try{
            return bd.scale() == 0 && bd.longValueExact() == 1L;
        }
        catch(Exception e){
            // The only way to end up here is that longValueExact throws an ArithmeticException,
            // so the amount is definitively not equal to 1.
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#scaleByPowerOfTen(int)
     */
    public FastMoney scaleByPowerOfTen(int n){
        return new FastMoney(getNumber().numberValue(BigDecimal.class).scaleByPowerOfTen(n), getCurrency(), true);
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#isZero()
     */
    public boolean isZero(){
        return this.number == 0L;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#isPositive()
     */
    public boolean isPositive(){
        return this.number > 0L;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#isPositiveOrZero()
     */
    public boolean isPositiveOrZero(){
        return this.number >= 0L;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#isNegative()
     */
    public boolean isNegative(){
        return this.number < 0L;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#isNegativeOrZero()
     */
    public boolean isNegativeOrZero(){
        return this.number <= 0L;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#getScale()
     */
    public int getScale(){
        return FastMoney.SCALE;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#getPrecision()
     */
    public int getPrecision(){
        return getNumber().numberValue(BigDecimal.class).precision();
    }

	/*
     * (non-Javadoc)
	 * @see javax.money.MonetaryAmount#signum()
	 */

    public int signum(){
        if(this.number < 0){
            return -1;
        }
        if(this.number == 0){
            return 0;
        }
        return 1;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#lessThan(javax.money.MonetaryAmount)
     */
    public boolean isLessThan(MonetaryAmount amount){
        checkAmountParameter(amount);
        return getBigDecimal().compareTo(amount.getNumber().numberValue(BigDecimal.class)) < 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#lessThan(java.lang.Number)
     */
    public boolean isLessThan(Number number){
        checkNumber(number);
        return getBigDecimal().compareTo(MoneyUtils.getBigDecimal(number)) < 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#lessThanOrEqualTo(javax.money.MonetaryAmount)
     */
    public boolean isLessThanOrEqualTo(MonetaryAmount amount){
        checkAmountParameter(amount);
        return getBigDecimal().compareTo(amount.getNumber().numberValue(BigDecimal.class)) <= 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#lessThanOrEqualTo(java.lang.Number)
     */
    public boolean isLessThanOrEqualTo(Number number){
        checkNumber(number);
        return getBigDecimal().compareTo(MoneyUtils.getBigDecimal(number)) <= 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#greaterThan(javax.money.MonetaryAmount)
     */
    public boolean isGreaterThan(MonetaryAmount amount){
        checkAmountParameter(amount);
        return getBigDecimal().compareTo(amount.getNumber().numberValue(BigDecimal.class)) > 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#greaterThan(java.lang.Number)
     */
    public boolean isGreaterThan(Number number){
        checkNumber(number);
        return getBigDecimal().compareTo(MoneyUtils.getBigDecimal(number)) > 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#greaterThanOrEqualTo(javax.money.MonetaryAmount ) #see
     */
    public boolean isGreaterThanOrEqualTo(MonetaryAmount amount){
        checkAmountParameter(amount);
        return getBigDecimal().compareTo(amount.getNumber().numberValue(BigDecimal.class)) >= 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#greaterThanOrEqualTo(java.lang.Number)
     */
    public boolean isGreaterThanOrEqualTo(Number number){
        checkNumber(number);
        return getBigDecimal().compareTo(MoneyUtils.getBigDecimal(number)) >= 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#isEqualTo(javax.money.MonetaryAmount)
     */
    public boolean isEqualTo(MonetaryAmount amount){
        checkAmountParameter(amount);
        return getBigDecimal().compareTo(amount.getNumber().numberValue(BigDecimal.class)) == 0;
    }

    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmount#hasSameNumberAs(java.lang.Number)
     */
    public boolean hasSameNumberAs(Number number){
        checkNumber(number);
        try{
            return this.number == getInternalNumber(number, false);
        }
        catch(ArithmeticException e){
            return false;
        }
    }


    /**
     * Gets the number representation of the numeric value of this item.
     *
     * @return The {@link Number} represention matching best.
     */
    @Override
    public NumberValue getNumber(){
        return new DefaultNumberValue(getBigDecimal());
    }

    // Static Factory Methods

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString(){
        return currency.toString() + ' ' + getBigDecimal();
    }

    // Internal helper methods

    /**
     * Internal method to check for correct number parameter.
     *
     * @param number the number to be checked, including null..
     * @throws NullPointerException          If the number is null
     * @throws java.lang.ArithmeticException If the number exceeds the capabilities of this class.
     */
    protected void checkNumber(Number number){
        Objects.requireNonNull(number, "Number is required.");
        // numeric check for overflow...
        if(number.longValue() > MAX_BD.longValue()){
            throw new ArithmeticException("Value exceeds maximal value: " + MAX_BD);
        }
        BigDecimal bd = MoneyUtils.getBigDecimal(number);
        if(bd.precision() > MAX_BD.precision()){
            throw new ArithmeticException("Precision exceeds maximal precision: " + MAX_BD.precision());
        }
        if(bd.scale() > SCALE){
            Logger log = Logger.getLogger(getClass().getName());
            if(log.isLoggable(Level.FINEST)){
                log.finest("Scale exceeds maximal scale of FastMoney (" + SCALE +
                                   "), implicit rounding will be applied to " + number);
            }
        }
    }

    /*
     * }(non-Javadoc)
     * @see javax.money.MonetaryAmount#adjust(javax.money.AmountAdjuster)
     */
    @Override
    public FastMoney with(MonetaryOperator operator){
        Objects.requireNonNull(operator);
        try{
            return FastMoney.class.cast(operator.apply(this));
        }
        catch(ArithmeticException e){
            throw e;
        }
        catch(Exception e){
            throw new MonetaryException("Operator failed: " + operator, e);
        }
    }

    @Override
    public <R> R query(MonetaryQuery<R> query){
        Objects.requireNonNull(query);
        try{
            return query.queryFrom(this);
        }
        catch(MonetaryException | ArithmeticException e){
            throw e;
        }
        catch(Exception e){
            throw new MonetaryException("Query failed: " + query, e);
        }
    }

    public static FastMoney from(MonetaryAmount amount){
        if(FastMoney.class == amount.getClass()){
            return (FastMoney) amount;
        }
        return new FastMoney(amount.getNumber(), amount.getCurrency(), false);
    }

    private BigDecimal getBigDecimal(){
        return BigDecimal.valueOf(this.number).movePointLeft(SCALE);
    }

    @Override
    public FastMoney multiply(double amount){
        if(amount == 1.0){
            return this;
        }
        if(amount == 0.0){
            return new FastMoney(0, this.currency);
        }
        return new FastMoney(Math.round(this.number * amount), this.currency);
    }

    @Override
    public FastMoney divide(long amount){
        if(amount == 1L){
            return this;
        }
        return new FastMoney(this.number / amount, this.currency);
    }

    @Override
    public FastMoney divide(double number){
        if(number == 1.0d){
            return this;
        }
        return new FastMoney(Math.round(this.number / number), getCurrency());
    }

    @Override
    public FastMoney remainder(long number){
        return remainder(BigDecimal.valueOf(number));
    }

    @Override
    public FastMoney remainder(double amount){
        return remainder(new BigDecimal(String.valueOf(amount)));
    }

    @Override
    public FastMoney[] divideAndRemainder(long amount){
        return divideAndRemainder(BigDecimal.valueOf(amount));
    }

    @Override
    public FastMoney[] divideAndRemainder(double amount){
        return divideAndRemainder(new BigDecimal(String.valueOf(amount)));
    }

    @Override
    public FastMoney stripTrailingZeros(){
        return this;
    }

    @Override
    public FastMoney multiply(long multiplicand){
        if(multiplicand == 1){
            return this;
        }
        if(multiplicand == 0){
            return new FastMoney(0L, this.currency);
        }
        return new FastMoney(multiplicand * this.number, this.currency);
    }

    @Override
    public FastMoney divideToIntegralValue(long divisor){
        if(divisor == 1){
            return this;
        }
        return divideToIntegralValue(MoneyUtils.getBigDecimal(divisor));
    }

    @Override
    public FastMoney divideToIntegralValue(double divisor){
        if(divisor == 1.0){
            return this;
        }
        return divideToIntegralValue(MoneyUtils.getBigDecimal(divisor));
    }

    @Override
    public MonetaryAmountFactory<FastMoney> getFactory(){
        return new FastMoneyAmountFactory().setAmount(this);
    }

}
