package com.jaemin.officehour.util;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.jaemin.officehour.domain.Slot;


public class SlotGenerator {
    
    public static List<Slot> generateWeeklySlots(){
        List<Slot> slots = new ArrayList<>();

        LocalTime startTime = LocalTime.of(10,30);
        LocalTime endTime = LocalTime.of(18,0);

        for (DayOfWeek day : DayOfWeek.values()){

            if (day.getValue() >= DayOfWeek.MONDAY.getValue() && 
                day.getValue() <= DayOfWeek.FRIDAY.getValue()) {
                
                LocalTime current = startTime;

                int slotIndex = 0;
                while (current.isBefore(endTime)) {

                    LocalTime next = current.plusMinutes(90);

                    slots.add(new Slot(day, current, next, slotIndex));

                    current = next;
                    slotIndex++;
                }
            }
        }

        return slots;
    }
}
