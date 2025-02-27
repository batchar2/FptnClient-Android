package org.fptn.vpn.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CountryFlagsTest {

    @Test
    public void testGetCountryFlagByCountryCode() {
        // Test valid country codes
        assertEquals("🇺🇸", CountryFlags.getCountryFlagByCountryCode("US")); // United States
        assertEquals("🇬🇧", CountryFlags.getCountryFlagByCountryCode("GB")); // United Kingdom
        assertEquals("🇯🇵", CountryFlags.getCountryFlagByCountryCode("JP")); // Japan

        // Test invalid country codes
        assertNull(CountryFlags.getCountryFlagByCountryCode("U"));  // Too short
        assertNull(CountryFlags.getCountryFlagByCountryCode(null)); // Null input
    }

    @Test
    public void testGetCountryCode() {
        // Test valid country names
        assertEquals("US", CountryFlags.getCountryCode("United States"));
        assertEquals("GB", CountryFlags.getCountryCode("United Kingdom"));
        assertEquals("JP", CountryFlags.getCountryCode("Japan"));

        // Test invalid country names
        assertNull(CountryFlags.getCountryCode("Nonexistent Country"));
        assertNull(CountryFlags.getCountryCode(null)); // Null input

        // Test three letters code
        assertEquals("US", CountryFlags.getCountryCode("USA"));
        assertEquals("RU", CountryFlags.getCountryCode("RUS"));
    }

    @Test
    public void testGetCountryCodeFromHostName() {
        // Test valid country names
        assertEquals("US", CountryFlags.getCountryCodeFromHostName("USA-NewYork"));
        assertEquals("LV", CountryFlags.getCountryCodeFromHostName("Latvia-200"));
        assertEquals("NL", CountryFlags.getCountryCodeFromHostName("Netherlands-1"));
        assertEquals("EE", CountryFlags.getCountryCodeFromHostName("Estonia"));
    }

}