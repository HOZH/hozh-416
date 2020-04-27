package com.example.demo.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;
import java.util.Map;


/**
 * @author Hong Zheng
 * @created 19/03/2020 - 4:14 PM
 * @project hozh-416-server
 */
@Data
@ToString(exclude = {"county"})
@NoArgsConstructor
@Entity(name = "PRECINCTS")
@Table

public class Precinct {

    /**
     * primary key for PRECINCT_TBL
     */
    @Id
    @JsonProperty("precinctId")
    private String id;

    /**
     * flag to determine whether
     * this precinct is a ghost precinct
     */
    private boolean ghost;

    /**
     * flag to determine whether this
     * precinct contains multiple border error
     */
    private boolean multipleBorder;

    /**
     * String of coordinates -> geo data
     */
    @Column(length = 1024)
    private String coordinates;

    /**
     * county that this precinct belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("precincts")
    private County county;

    /**
     * election data of this precinct
     */
    @SuppressWarnings("JpaDataSourceORMInspection")
    @ElementCollection
    @CollectionTable(name = "ELECTION_DATA")
    private Map<ElectionEnum, Integer> electionData;

    /**
     * list of precinct's ids for which adjacent to
     * this precinct
     */
    @SuppressWarnings("JpaDataSourceORMInspection")
    @ElementCollection
    @CollectionTable(name = "ADJACENT_PRECINCTS")
    @Column(name = "adjacent_precinct_ids")
    private List<String> adjPrecIds;

    /**
     * list of precinct's ids for which enclosing to
     * this precinct -> used for determine errors
     */
    @SuppressWarnings("JpaDataSourceORMInspection")
    @ElementCollection
    @CollectionTable(name = "ENCLOSING_PRECINCTS")
    @Column(name = "enclosing_precinct_ids")
    private List<String> enclPrecIds;

    /**
     * map for log messages
     */
    @SuppressWarnings("JpaDataSourceORMInspection")
    @ElementCollection
    @CollectionTable(name = "LOG_BAG")
    private Map<Integer, String> logBag;

    /**
     * following are the help fields of the object which won't be persist in the database
     */

    @Transient
    private String canonicalName;

    /**
     * help field for mapping the precinct to its belonging county
     */
    @Transient
    private String stateId;

    /**
     * help field for mapping the precinct's belonging county to its belonging state
     */
    @Transient
    private String countyId;

    /**
     * flag to determine whether to update this precinct's belonging county's demographic data
     */
    @Transient
    private boolean demoModified;
    ;

    /**
     * following are the demographic population help fields,
     * can be ignore if demographicDataModified is set to false
     */
    @Transient
    private int white;

    @Transient
    private int africanAmer;

    @Transient
    private int asian;

    @Transient
    private int nativeAmer;

    @Transient
    private int others;

    @Transient
    private int pasifika;

}