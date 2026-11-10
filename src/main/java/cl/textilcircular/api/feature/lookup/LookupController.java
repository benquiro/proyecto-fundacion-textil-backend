package cl.textilcircular.api.feature.lookup;

import cl.textilcircular.api.core.repository.AgeRangeRepository;
import cl.textilcircular.api.core.repository.CommuneRepository;
import cl.textilcircular.api.core.repository.GenderRepository;
import cl.textilcircular.api.core.repository.RegionRepository;
import cl.textilcircular.api.core.repository.TextileServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reference lists the app caches: public, read only. */
@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupController {

    private final RegionRepository regions;
    private final CommuneRepository communes;
    private final TextileServiceRepository services;
    private final AgeRangeRepository ageRanges;
    private final GenderRepository genders;

    public record RegionResponse(long id, String name) {
    }

    public record CommuneResponse(long id, long regionId, String name) {
    }

    public record ServiceResponse(long id, String name) {
    }

    public record DescribedResponse(long id, String description) {
    }

    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return regions.findAllByOrderBySortOrderAsc().stream()
                .map(r -> new RegionResponse(r.getId(), r.getName())).toList();
    }

    @GetMapping("/communes")
    public List<CommuneResponse> communes(@RequestParam(required = false) Long regionId) {
        var found = regionId == null ? communes.findAllByOrderByNameAsc() : communes.findByRegionIdOrderByNameAsc(regionId);
        return found.stream().map(c -> new CommuneResponse(c.getId(), c.getRegion().getId(), c.getName())).toList();
    }

    @GetMapping("/services")
    public List<ServiceResponse> services() {
        return services.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new ServiceResponse(s.getId(), s.getName())).toList();
    }

    @GetMapping("/age-ranges")
    public List<DescribedResponse> ageRanges() {
        return ageRanges.findAllByOrderBySortOrderAsc().stream()
                .map(a -> new DescribedResponse(a.getId(), a.getDescription())).toList();
    }

    @GetMapping("/genders")
    public List<DescribedResponse> genders() {
        return genders.findAllByOrderByIdAsc().stream()
                .map(g -> new DescribedResponse(g.getId(), g.getDescription())).toList();
    }
}
