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

    @Schema(name = "DescribedItem", description = "Rango etario o género")
    public record DescribedResponse(@Schema(example = "1") long id, @Schema(example = "18 a 24") String description) {
    }

    @Operation(tags = "Lookup", summary = "Listar regiones", description = "En el orden en que la app las muestra.")
    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return regions.findAllByOrderBySortOrderAsc().stream()
                .map(r -> new RegionResponse(r.getId(), r.getName())).toList();
    }

    @Operation(tags = "Lookup", summary = "Listar comunas", description = "En orden alfabético; filtra por región con `regionId`.")
    @GetMapping("/communes")
    public List<CommuneResponse> communes(@Parameter(description = "Solo las comunas de esta región") @RequestParam(required = false) Long regionId) {
        var found = regionId == null ? communes.findAllByOrderByNameAsc() : communes.findByRegionIdOrderByNameAsc(regionId);
        return found.stream().map(c -> new CommuneResponse(c.getId(), c.getRegion().getId(), c.getName())).toList();
    }

    @Operation(tags = "Lookup", summary = "Listar servicios textiles", description = "Solo los servicios activos, en orden alfabético.")
    @GetMapping("/services")
    public List<ServiceResponse> services() {
        return services.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new ServiceResponse(s.getId(), s.getName())).toList();
    }

    @Operation(tags = "Lookup", summary = "Listar rangos etarios", description = "En el orden en que la app los muestra.")
    @GetMapping("/age-ranges")
    public List<DescribedResponse> ageRanges() {
        return ageRanges.findAllByOrderBySortOrderAsc().stream()
                .map(a -> new DescribedResponse(a.getId(), a.getDescription())).toList();
    }

    @Operation(tags = "Lookup", summary = "Listar géneros", description = "Ordenados por id.")
    @GetMapping("/genders")
    public List<DescribedResponse> genders() {
        return genders.findAllByOrderByIdAsc().stream()
                .map(g -> new DescribedResponse(g.getId(), g.getDescription())).toList();
    }
}
