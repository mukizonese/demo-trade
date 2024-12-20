package com.tradingzone.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class RandomTest {
    @Test
    void random() {

        Random r = new Random();
        double randomValue = r.nextDouble()*5;
        double oldLTP = 514.02;
        double rangeMin = oldLTP - randomValue;
        double rangeMax = oldLTP + randomValue;
        //double randomRangeValue = rangeMin + (rangeMax - rangeMin) * randomValue;
        //((Math.random() * (max - min)) + min);
        double randomRangeValue = ((randomValue * (rangeMax - rangeMin)) + rangeMin);
        //double newLTP = Double.valueOf(randomRangeValue);
        BigDecimal bd = new BigDecimal(randomRangeValue).setScale(2, RoundingMode.HALF_UP);
        double newLTP = bd.doubleValue();
        System.out.println(" Old " + oldLTP) ;
        System.out.println(" randomValue " + randomValue) ;
        System.out.println(" rangeMin " + rangeMin) ;
        System.out.println(" rangeMax " + rangeMax) ;
        System.out.println(" randomRangeValue " + randomRangeValue) ;
        System.out.println(" newLTP " + newLTP) ;


    }
}
