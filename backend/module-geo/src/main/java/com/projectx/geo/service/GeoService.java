package com.projectx.geo.service;

import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.geo.domain.Country;
import com.projectx.geo.domain.District;
import com.projectx.geo.domain.Region;
import com.projectx.geo.domain.State;
import com.projectx.geo.dto.CountryResponse;
import com.projectx.geo.dto.DistrictResponse;
import com.projectx.geo.dto.RegionResponse;
import com.projectx.geo.dto.StateResponse;
import com.projectx.geo.repo.CountryRepository;
import com.projectx.geo.repo.DistrictRepository;
import com.projectx.geo.repo.RegionRepository;
import com.projectx.geo.repo.StateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GeoService {

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;

    public GeoService(CountryRepository countryRepository, StateRepository stateRepository,
                       RegionRepository regionRepository, DistrictRepository districtRepository) {
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.regionRepository = regionRepository;
        this.districtRepository = districtRepository;
    }

    public List<CountryResponse> listCountries() {
        return countryRepository.findAllByOrderByNameAsc().stream().map(CountryResponse::from).toList();
    }

    public List<StateResponse> listStates(Long countryId) {
        return stateRepository.findByCountryIdOrderByNameAsc(countryId).stream().map(StateResponse::from).toList();
    }

    /** Regions are optional per state (see Region's javadoc) - an empty list here is normal, not an error; the frontend falls back to State -> District directly. */
    public List<RegionResponse> listRegions(Long stateId) {
        return regionRepository.findByStateIdOrderByNameAsc(stateId).stream().map(RegionResponse::from).toList();
    }

    /** Exactly one of stateId/regionId should be supplied - regionId narrows further within a state that has regions defined. */
    public List<DistrictResponse> listDistricts(Long stateId, Long regionId) {
        List<District> districts = regionId != null
                ? districtRepository.findByRegionIdOrderByNameAsc(regionId)
                : districtRepository.findByStateIdOrderByNameAsc(stateId);
        return districts.stream().map(this::toDistrictResponse).toList();
    }

    /** Powers "type a district name, auto-fill everything above it" - each hit carries its full ancestor chain already resolved. */
    public List<DistrictResponse> searchDistricts(String query) {
        return districtRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(query).stream()
                .map(this::toDistrictResponse)
                .toList();
    }

    private DistrictResponse toDistrictResponse(District district) {
        State state = stateRepository.findById(district.getStateId())
                .orElseThrow(() -> ResourceNotFoundException.of("State", district.getStateId()));
        Country country = countryRepository.findById(state.getCountryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Country", state.getCountryId()));
        Region region = district.getRegionId() != null ? regionRepository.findById(district.getRegionId()).orElse(null) : null;

        return new DistrictResponse(
                district.getId(), district.getName(),
                state.getId(), state.getName(),
                region != null ? region.getId() : null, region != null ? region.getName() : null,
                country.getId(), country.getName());
    }
}
