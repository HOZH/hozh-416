package com.example.demo.service;

import com.example.demo.entity.County;
import com.example.demo.entity.Precinct;
import com.example.demo.entitymanager.PrecinctEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Hong Zheng
 * @created 19/03/2020 - 4:14 PM
 * @project hozh-416-server
 */

@Service
public class PrecinctService {

    private final PrecinctEntityManager pem;
    private final CountyService cs;

    @Autowired
    public PrecinctService(PrecinctEntityManager pem, CountyService cs) {
        this.pem = pem;
        this.cs = cs;
    }

    /**
     * query a precinct by the given id
     *
     * @param id -> String type, using as a id to query the target precinct
     * @return query result by given id -> type Precinct, return null if illegal arg exception raised
     */
    public Precinct selectPrecinctById(String id) {
        try {
            return pem.findById(id).orElse(null);
        } catch (Exception ex) {
            //fixme for now we may encounter Illegal arg exception, change generic handler to more concrete one later
            System.err.println(ex.getMessage());
            return null;
        }
    }

    /**
     * return a collection of all the precinct records in the database
     *
     * @return query result -> type List<Precinct>
     */
    public List<Precinct> selectAllPrecincts() {
        return pem.findAll();
    }

    /**
     * delete a precinct record by the given id
     *
     * @param id -> String type, using as a id to query the target precinct
     */
    public void deletePrecinctById(String id) {
        pem.deleteById(id);
    }

    /**
     * update a precinct record in database.
     *
     * @param precinct -> precinct type
     * @return the saved precinct entity -> type precinct, return null if null pointer/ illegal arg exception raised
     * @see this.updateNeighbors
     */
    public Precinct updatePrecinct(Precinct precinct) {
        try {
            // getCountyId is never going to be null by convention in our group
            County targetCounty = cs.selectCountyById(precinct.getCountyId());


            // nullity check has been done in first selection
            // pull up the precinct record of target precinct in database
            var precinctRecord = pem.findById(precinct.getId()).orElse(null);

            // comparing the adjacentPrecinctIds of the updated precinct and the record in the database
            if (precinctRecord.getAdjPrecIds().containsAll(precinct.getAdjPrecIds()) && precinctRecord.getAdjPrecIds().size() == precinct.getAdjPrecIds().size()) {

                // if the adjacentPrecinctIds of target precinct is not changed then check is demographic data modified for its county
                if (precinct.isDemoModified()) {
                    updateEthnicityDataHelper(targetCounty, precinct);
                    cs.saveCounty(targetCounty);
                }
                precinct.setCounty(targetCounty);

                // save the target precinct into the database
                return pem.save(precinct);

            } else {
                //else go to helper method updateNeighbors
                return updateNeighbors(precinct);
            }

        } catch (Exception ex) {

            //fixme may encounter nested exception, need a more concert error handler for that
            System.err.println("precinct adjacentPrecinctIds is null");
            System.err.println(ex.getMessage());
            return null;
        }
    }

    /**
     * add a precinct object into database.
     *
     * @param precinct -> precinct type
     * @return the saved precinct entity -> type precinct, return null if null pointer/ illegal arg exception raised
     */
    public Precinct addPrecinct(Precinct precinct) {
        try {
            // getCountyId is never going to be null by convention in our group
            County targetCounty = cs.selectCountyById(precinct.getCountyId());


            var countyNotFound = targetCounty == null;

            // query current precinct's belonging county
            // if the belonging county is not found in database then create the county with county id and ethnicity
            // data wrapped in the current precinct
            if (countyNotFound) {
                targetCounty = new County();
                targetCounty.setId(precinct.getCountyId());
                targetCounty.setStateId(precinct.getStateId());
            }

            // if the belonging county is not found in database or the flag for updating demographic data in
            // current precinct is set to true then update ethnicity data wrapped in the current precinct to
            // current county
            if (countyNotFound || precinct.isDemoModified()) {
                updateEthnicityDataHelper(targetCounty, precinct);
                cs.saveCounty(targetCounty);
            }

            // set the county field for target precinct
            precinct.setCounty(targetCounty);

            // if the precinct id is not given then generate a random string id for the precinct in uuid v4 format
            if (precinct.getId() == null) {
                precinct.setId(UUID.randomUUID().toString());
            }

            // save the target precinct into the database
            var result = pem.save(precinct);

            // inform the target precinct's adjacent precincts to add it to their adjacent precinct id list
            pem.findAllById(precinct.getAdjPrecIds()).forEach(e -> {

                // if not already include then add to the target's list and save the changes
                if (!e.getAdjPrecIds().contains(result.getId())) {

                    e.getAdjPrecIds().add(result.getId());
                    pem.save(e);
                }
            });

            return result;
//            }

        } catch (Exception ex) {

            //fixme may encounter nested exception, need a more concert error handler for that
            System.err.println("precinct adjacentPrecinctIds is null");
            System.err.println(ex.getMessage());
            return null;
        }
    }


    /**
     * helper method for updating a precinct, it will update the adjacentPrecinctIds list of target
     * precinct and its adjacent precincts bidirectionally
     *
     * @param precinct -> precinct type
     * @return the saved precinct entity -> type precinct, return null if null pointer/ illegal arg exception raised
     */
    public Precinct updateNeighbors(Precinct precinct) {

        try {
            // pull up record of target precinct in the database
            // nullity check has been done in the method calling this
            var precinctRecord = pem.findById(precinct.getId()).orElse(null);

            // set diff of adjacentPrecinctIds from the record in database and current precinct
            ArrayList<String> deleted = new ArrayList(precinctRecord.getAdjPrecIds());

            // set diff of adjacentPrecinctIds from current precinct and the record in database
            ArrayList<String> added = new ArrayList(precinct.getAdjPrecIds());

            // adjacentPrecinctIds cannot be null by convention
            deleted.remove(new ArrayList(precinct.getAdjPrecIds()));
            added.remove(new ArrayList(precinctRecord.getAdjPrecIds()));

            // removing target precinct's id from its deleted precinct ids in their adjacent precinct ids list
            for (var i : pem.findAllById(deleted)) {
                i.getAdjPrecIds().remove(precinct.getId());
                pem.save(i);
            }

            // adding target precinct's id to its newly added precinct ids in their adjacent precinct ids list
            for (var i : pem.findAllById(added)) {
                i.getAdjPrecIds().add(precinct.getId());
                pem.save(i);
            }

            // if the adjacentPrecinctIds of target precinct is not changed then check is demographic data modified for its county
            if (precinct.isDemoModified()) {
                var targetCounty = cs.selectCountyById(precinct.getCountyId());

                updateEthnicityDataHelper(targetCounty, precinct);
                precinct.setCounty(targetCounty);
                cs.saveCounty(targetCounty);
            }
            return pem.save(precinct);
        } catch (NullPointerException | IllegalArgumentException ex) {
            System.err.println("precinct adjacentPrecinctIds is null");
            System.err.println(ex.getMessage());
            return null;
        }
    }

    /**
     * merging two precincts
     *
     * @param precincts -> type List<Precinct>, index 0 -> primary precinct, index 1 deleting precinct
     * @return Precinct object of survived precinct
     */
    public Precinct mergePrecincts(List<Precinct> precincts) {

        try {

            // primary precinct
            Precinct primaryPrecinct = precincts.get(0);
            // deleting precinct
            Precinct deletingPrecinct = precincts.get(1);

            // merge two's adjacent list and delete the deleting precinct's id from its adjacent precinct ids
            deletingPrecinct.getAdjPrecIds().forEach(e -> {

                // precinct queried by the ids in deleting precincts' adjacent precinct ids list
                var temp = pem.findById(e).orElse(null);

                // if the primary precinct not already contained the temp then
                if (!primaryPrecinct.getAdjPrecIds().contains(e)) {

                    // if temp is not primary precinct, add each other to their id to their adjacent precinct ids
                    if (!e.equals(primaryPrecinct.getId())) {
                        primaryPrecinct.getAdjPrecIds().add(e);
                        temp.getAdjPrecIds().add(primaryPrecinct.getId());
                    }
                }
                temp.getAdjPrecIds().remove(deletingPrecinct.getId());
                pem.save(temp);
            });

            // deleting precinct's id from temp's list
            primaryPrecinct.getAdjPrecIds().remove(deletingPrecinct.getId());

            // remove deleting precinct from database
            pem.deleteById(deletingPrecinct.getId());

            // save primaryPrecinct precinct
            return pem.save(primaryPrecinct);

        } catch (NullPointerException ex) {
            System.err.println("precinct adjacentPrecinctIds is null");
            System.err.println(ex.getMessage());
            return null;
        }
    }

    /**
     * set ethnicity data of a County object to ethnicity data of a Precinct object
     *
     * @param c -> type County, p -> type Precinct
     */
    private void updateEthnicityDataHelper(County c, Precinct p) {
        c.setWhite(p.getWhite());
        c.setAfricanAmer(p.getAfricanAmer());
        c.setAsian(p.getAsian());
        c.setNativeAmer(p.getNativeAmer());
        c.setPasifika(p.getPasifika());
        c.setOthers(p.getOthers());
    }
}
