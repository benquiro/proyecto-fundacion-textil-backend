package cl.textilcircular.api.feature.lookup;

import cl.textilcircular.api.core.repository.AgeRangeRepository;
import cl.textilcircular.api.core.repository.CommuneRepository;
import cl.textilcircular.api.core.repository.GenderRepository;
import cl.textilcircular.api.core.repository.RegionRepository;
import cl.textilcircular.api.core.repository.TextileServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reference lists the app caches: public, read only. */
@SecurityRequirements
@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupController {

    private final RegionRepository regions;
    private final CommuneRepository communes;
    private final TextileServiceRepository services;
    private final AgeRangeRepository ageRanges;
    private final GenderRepository genders;

    @Schema(name = "Region")
    public record RegionResponse(@Schema(example = "13") long id, @Schema(example = "Región Metropolitana") String name) {
    }

    @Schema(name = "Commune")
    public record CommuneResponse(@Schema(example = "101") long id, @Schema(example = "13") long regionId,
                                  @Schema(example = "Santiago") String name) {
    }

    @Schema(name = "TextileService")
    public record ServiceResponse(@Schema(example = "1") long id, @Schema(example = "Arreglos") String name) {
    }

    @Schema(name = "DescribedItem", description = "Age range or gender")
    public record DescribedResponse(@Schema(example = "1") long id, @Schema(example = "18 a 24") String description) {
    }

    @Operation(tags = "Lookup", summary = "List regions", description = "Ordered as the app shows them.")
    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return regions.findAllByOrderBySortOrderAsc().stream()
                .map(r -> new RegionResponse(r.getId(), r.getName())).toList();
    }

    @Operation(tags = "Lookup", summary = "List communes", description = "Alphabetical; filter by region with `regionId`.")
    @GetMapping("/communes")
    public List<CommuneResponse> communes(@Parameter(description = "Only communes of this region") @RequestParam(required = false) Long regionId) {
        var found = regionId == null ? communes.findAllByOrderByNameAsc() : communes.findByRegionIdOrderByNameAsc(regionId);
        return found.stream().map(c -> new CommuneResponse(c.getId(), c.getRegion().getId(), c.getName())).toList();
    }

    @Operation(tags = "Lookup", summary = "List textile services", description = "Only active services, alphabetical.")
    @GetMapping("/services")
    public List<ServiceResponse> services() {
        return services.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new ServiceResponse(s.getId(), s.getName())).toList();
    }

    @Operation(tags = "Lookup", summary = "List age ranges", description = "Ordered as the app shows them.")
    @GetMapping("/age-ranges")
    public List<DescribedResponse> ageRanges() {
        return ageRanges.findAllByOrderBySortOrderAsc().stream()
                .map(a -> new DescribedResponse(a.getId(), a.getDescription())).toList();
    }

    @Operation(tags = "Lookup", summary = "List genders", description = "Ordered by id.")
    @GetMapping("/genders")
    public List<DescribedResponse> genders() {
        return genders.findAllByOrderByIdAsc().stream()
                .map(g -> new DescribedResponse(g.getId(), g.getDescription())).toList();
    }
}
