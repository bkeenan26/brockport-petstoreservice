package com.petstoreservices.service;


import com.petstore.PetEntity;

import com.petstore.animals.attributes.PetType;
import com.petstore.exceptions.DuplicatePetStoreRecordException;
import com.petstore.exceptions.PetNotFoundSaleException;
import com.petstore.exceptions.PetTypeNotSupportedException;
import com.petstoreservices.exceptions.*;
import com.petstoreservices.repository.PetRepository;

import org.springframework.stereotype.Service;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Pet Inventory Service.  This service currently supports
 * Adding a single pet, removing a single pet, and retrieving the list/
 * The service also keeps up to data an inventory data de-limited file
 */
@Service
public class PetInventoryService {

    private final PetRepository petRepo;

    public PetInventoryService()
    {

        petRepo = new PetRepository();
    }

    /**
     *  Retrieve the pet store Inventory of pets
     * @return
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    public List<PetEntity> getInventory() throws PetDataStoreException {
        List<PetEntity> inventoryList = this.petRepo.getPetInventory();
        return inventoryList;
    }

    /**
     * Identify and return pet by it's type and unique id
     * @param petType - type of pet searching for
     * @param petId - unique id per pet type
     * @return - found pet
     * @throws PetNotFoundSaleException - pet does not exist in inventory
     * @throws DuplicatePetStoreRecordException - duplicate record found
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    public PetEntity findPetByIdAndType(PetType petType, int petId) throws PetNotFoundSaleException,
            DuplicatePetStoreRecordException, PetDataStoreException {
        return this.findPetByPetTypeAndPetId(petType, petId);
    }
    /**
     * Add a new item to the inventory list
     * @param petType - type of pet to be added
     * @param newPetRestItem - the Rest Request body representation
     * @return - return the Pet
     * @throws PetTypeNotSupportedException - Pet type is not supported by pet store ex{WILD and FARM}
     * @throws PetStoreAnimalTypeException - Invalid Pet store type
     * @throws PetInventoryFileNotCreatedException - pet inventory file could not be created
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    public PetEntity addInventory(PetType petType, PetEntity newPetRestItem)
            throws PetStoreAnimalTypeException, PetTypeNotSupportedException,
        PetInventoryFileNotCreatedException, PetDataStoreException {
        List<PetEntity> sortedPets = this.petRepo.getPetInventory().stream()
                .filter(p -> p.getPetType().equals(petType))
                .sorted(Comparator.comparingInt(PetEntity::getPetId))
                .collect(Collectors.toList());
        newPetRestItem.setPetType(petType);
        return  this.petRepo.createPetEntity(newPetRestItem,sortedPets);
    }

    /**
     * Remove a pet item inventory item from list
     * @param petType - Need the PetType to filter the store
     * @param petId - each pet id is unique per pet type
     * @return - Return the Pet found
     * @throws DuplicatePetStoreRecordException - Duplicate record exists
     * @throws PetNotFoundSaleException - Pet search return nothing
     * @throws PetInventoryFileNotCreatedException - file not created
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    public PetEntity removeInventoryByIDAndPetType(PetType petType, int petId)
            throws DuplicatePetStoreRecordException, PetNotFoundSaleException,
            PetInventoryFileNotCreatedException, PetDataStoreException {
        PetEntity removeItem = this.petRepo.removeEntity(this.findPetByPetTypeAndPetId(petType, petId));
        return removeItem;
    }

    /**
     * Search list for a pet type and pet Id
     * @param petType - Need the PetType to filter the store
     * @param petId - each pet id is unique per pet type
     * @return - Return the Pet found
     * @throws PetNotFoundSaleException - Pet is not found
     * @throws DuplicatePetStoreRecordException - Duplicate record found in the store
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    private PetEntity findPetByPetTypeAndPetId(PetType petType, int petId) throws PetNotFoundSaleException,
            DuplicatePetStoreRecordException, PetDataStoreException {
        List<PetEntity> filteredPets =  this.petRepo.getPetInventory().stream()
                .filter(p -> p.getPetType().equals(petType))
                .filter(id -> id.getPetId()==petId)
                .collect(Collectors.toList());
        if(filteredPets.isEmpty())
        {
            throw new PetNotFoundSaleException("0 results found for search criteria for pet id[" + petId + "] " +
                    "petType[" + petType +"] Please try again!!");
        }
        else if(filteredPets.size() >1)
        {
            throw new DuplicatePetStoreRecordException("Should only of retrieved one item from the pet store and " +
                    "found duplicate records in the results.  This list size was[" + filteredPets.size() +
                    "] for pet id[" + petId + "] petType[" + petType +"]");
        }
        else{
            return filteredPets.get(0);
        }

    }

    /**
     * Search petsForSale list for matches to PetType
     * @param petType - the type of pet
     * @return - all the pets by pet type found
     * @throws PetNotFoundSaleException - Pet type not found in inventory
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    public List<PetEntity> getPetsByPetType(PetType petType) throws PetNotFoundSaleException,  PetDataStoreException {
        List<PetEntity> sortedPets = this.petRepo.getPetInventory().stream()
                .filter(p -> p.getPetType().equals(petType))
                .sorted(Comparator.comparingInt(p->p.getPetId()))
                .collect(Collectors.toList());
        if(sortedPets.isEmpty())
        {
            throw new PetNotFoundSaleException("0 results found for search criteria petType[" + petType
                    +"] Please try again!!");
        }
        return sortedPets;
    }

    /**
     * Update the inventory by Pet id and Type.  Update the data file
     * @param petType - the type of pet
     * @param petId - the unique id of the pet
     * @param petItemUpdate - The RestItem representation of Pet -
     * @return - the pet item updated
     * @throws DuplicatePetStoreRecordException - There is more than 1 pet store record for that pet type with same id
     * @throws PetTypeNotSupportedException - Pet Type is not supported so cannot update the inventory
     * @throws UpdatePetException - the body is not what is expected
     * @throws PetDataStoreException - Issue with file format, reading the file, or file is not present
     */
    public PetEntity updateInventoryByPetIdAndPetType(PetType petType, int petId, PetEntity petItemUpdate)
            throws  DuplicatePetStoreRecordException, PetTypeNotSupportedException, UpdatePetException,
            PetStoreAnimalTypeException, PetInventoryFileNotCreatedException, PetDataStoreException {
        PetEntity updatedPetItem = null;
        try{
            updatedPetItem = this.findPetByPetTypeAndPetId(petType, petId);
            this.petRepo.removeEntity(updatedPetItem);
            updatedPetItem = this.petRepo.updatePetEntity(petItemUpdate, updatedPetItem);

        }catch(PetNotFoundSaleException e)
        {
            System.out.println("Item Not Found so adding with new petstore id");// should use a logger
            updatedPetItem = this.addInventory(petType, petItemUpdate);
        }

        return updatedPetItem;
    }
}