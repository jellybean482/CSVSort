package com.jia.csv.test;

import static org.junit.Assert.assertTrue;

import com.jia.csv.CSVSorter;

import org.junit.Assert;
import org.junit.Test;

public class CSVSorterUnitTest {

    @Test
    public void splitHeader() {
        String[] expected = new String[] {"regular", "string"};
        StringBuilder sb = new StringBuilder(String.join(",", expected));
        String[] actual = CSVSorter.splitHeader(sb.toString());
        Assert.assertArrayEquals(expected, actual);
    }
    
    @Test
    public void splitTwoEmptyValues() {
        String[] emptyStrings = new String[] {"", ""};
        StringBuilder sb = new StringBuilder(String.join(",", emptyStrings));
        String[] actual = CSVSorter.splitValue(sb.toString());
        Assert.assertEquals(0, actual.length);
    }
    
    @Test
    public void splitAllValues() {
        String[] expected = new String[] {"John, Smith", "Else"};
        StringBuilder sb = new StringBuilder(String.join(",", expected));
        String[] actual = CSVSorter.splitValue(sb.toString());
        Assert.assertArrayEquals(expected, actual);
    }
    
    @Test
    public void parseValue() {
        String expected = "string";
        Object actual = CSVSorter.parseValue(expected);
        assertTrue(actual instanceof String);
        Assert.assertEquals(expected, (String)actual);
    }
    
    @Test
    public void parseValueInteger() {
        int expected = 123;
        Object actual = CSVSorter.parseValue(String.valueOf(expected));
        assertTrue(actual instanceof Integer);
        Assert.assertEquals(expected, ((Integer)actual).intValue());
    }
    
    @Test
    public void parseValueDouble() {
        double expected = 123.456;
        Object actual = CSVSorter.parseValue(String.valueOf(expected));
        assertTrue(actual instanceof Double);
        Assert.assertEquals(expected, ((Double)actual).doubleValue(), 0.001);
    }
    
    @Test
    public void parseInteger() {
        int expected = 123;
        Integer actual = CSVSorter.parseInteger(String.valueOf(expected));
        Assert.assertEquals(expected, actual.intValue());
    }
    
    @Test
    public void parseDouble() {
        double expected = 123.456;
        Double actual = CSVSorter.parseDouble(String.valueOf(expected));
        Assert.assertEquals(expected, actual.doubleValue(), 0.001);
    }
    
}
