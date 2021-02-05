/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sheets;

import java.util.NoSuchElementException;

//Name
//Tonic
//Tonality
//Melodic Range
//Harmonic Level
//Harmonization Type
//recorded voice/piano
//recorded piano (full)
//recorded piano (chords)
//recorded piano melody

/**
 *
 * @author Admin
 */
public class Tune {
    
    private String phoneNumber;
    private String name;
    private String skill;
    private String address;

   
    public Tune (String lineDetail) throws NoSuchElementException{
        
        try {
            String[] params = lineDetail.split("\\^",-2);

            phoneNumber = params[0];
            name = params[1];
            skill = params[2];
            address = params[3];
        } catch (ArrayIndexOutOfBoundsException aioe) {
            throw new NoSuchElementException("User file corrupted");
        }
        
    }
    
    public String toString() {
        return getPhoneNumber() + "^" + getName() + "^" + getSkill();
    }

    /**
     * @return the phoneNumber
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the skill
     */
    public String getSkill() {
        return skill;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    
}
