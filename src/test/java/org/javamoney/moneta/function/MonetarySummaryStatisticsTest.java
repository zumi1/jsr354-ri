package org.javamoney.moneta.function;

import static org.javamoney.moneta.function.StreamFactory.BRAZILIAN_REAL;
import static org.javamoney.moneta.function.StreamFactory.DOLLAR;

import javax.money.MonetaryException;

import junit.framework.Assert;

import org.javamoney.moneta.Money;
import org.testng.annotations.Test;

public class MonetarySummaryStatisticsTest {

	@Test
	public void shouldBeEmpty() {
		MonetarySummaryStatistics summary = new MonetarySummaryStatistics(
				BRAZILIAN_REAL);
		Assert.assertEquals(0L, summary.getCount());
		Assert.assertEquals(0L, summary.getMin().getNumber().longValue());
		Assert.assertEquals(0L, summary.getMax().getNumber().longValue());
		Assert.assertEquals(0L, summary.getSum().getNumber().longValue());
		Assert.assertEquals(0L, summary.getAvarage().getNumber().longValue());

	}

	@Test(expectedExceptions = NullPointerException.class)
	public void shouldErrorWhenIsNull() {
		MonetarySummaryStatistics summary = new MonetarySummaryStatistics(
				BRAZILIAN_REAL);
		summary.accept(null);
	}

	@Test(expectedExceptions = MonetaryException.class)
	public void shouldErrorWhenIsDifferentCurrency() {
		MonetarySummaryStatistics summary = new MonetarySummaryStatistics(
				BRAZILIAN_REAL);
		summary.accept(Money.of(10, DOLLAR));
	}

	@Test
	public void shouldBeSameValueWhenOneMonetaryIsAdded() {
		MonetarySummaryStatistics summary = new MonetarySummaryStatistics(
				BRAZILIAN_REAL);
		summary.accept(Money.of(10, BRAZILIAN_REAL));
		Assert.assertEquals(1L, summary.getCount());
		Assert.assertEquals(10L, summary.getMin().getNumber().longValue());
		Assert.assertEquals(10L, summary.getMax().getNumber().longValue());
		Assert.assertEquals(10L, summary.getSum().getNumber().longValue());
		Assert.assertEquals(10L, summary.getAvarage().getNumber().longValue());
	}

	@Test
	public void addTest() {
		MonetarySummaryStatistics summary = createSummary();
		Assert.assertEquals(3L, summary.getCount());
		Assert.assertEquals(10L, summary.getMin().getNumber().longValue());
		Assert.assertEquals(110L, summary.getMax().getNumber().longValue());
		Assert.assertEquals(210L, summary.getSum().getNumber().longValue());
		Assert.assertEquals(70L, summary.getAvarage().getNumber().longValue());
	}

	@Test
	public void combineTest() {
		MonetarySummaryStatistics summaryA = createSummary();
		MonetarySummaryStatistics summaryB = createSummary();
		summaryA.combine(summaryB);
		Assert.assertEquals(6L, summaryA.getCount());
		Assert.assertEquals(10L, summaryA.getMin().getNumber().longValue());
		Assert.assertEquals(110L, summaryA.getMax().getNumber().longValue());
		Assert.assertEquals(420L, summaryA.getSum().getNumber().longValue());
		Assert.assertEquals(70L, summaryA.getAvarage().getNumber().longValue());
	}

	private MonetarySummaryStatistics createSummary() {
		MonetarySummaryStatistics summary = new MonetarySummaryStatistics(
				BRAZILIAN_REAL);
		summary.accept(Money.of(10, BRAZILIAN_REAL));
		summary.accept(Money.of(90, BRAZILIAN_REAL));
		summary.accept(Money.of(110, BRAZILIAN_REAL));
		return summary;
	}

}
