package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.reference.AgeRange;
import cl.textilcircular.api.core.model.reference.Commune;
import cl.textilcircular.api.core.model.reference.Gender;
import cl.textilcircular.api.core.model.reference.Region;
import cl.textilcircular.api.core.model.reference.TextileService;
import cl.textilcircular.api.support.RepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class ReferenceRepositoriesTest {

    @Autowired RegionRepository regions;
    @Autowired CommuneRepository communes;
    @Autowired TextileServiceRepository services;
    @Autowired AgeRangeRepository ageRanges;
    @Autowired GenderRepository genders;

    @Test
    void referenceDataMatchesTheAppSeed() {
        assertThat(regions.count()).isEqualTo(16);
        assertThat(communes.count()).isEqualTo(346);
        assertThat(services.count()).isEqualTo(7);
        assertThat(ageRanges.count()).isEqualTo(7);
        assertThat(genders.count()).isEqualTo(4);
    }

    @Test
    void regionsComeInGeographicOrder() {
        List<Region> all = regions.findAllByOrderBySortOrderAsc();

        assertThat(all.getFirst().getName()).isEqualTo("Arica y Parinacota");
        assertThat(all).extracting(Region::getSortOrder).isSorted();
    }

    @Test
    void communesAreListedByRegionAndName() {
        List<Commune> arica = communes.findByRegionIdOrderByNameAsc(1L);

        assertThat(arica).extracting(Commune::getName)
                .containsExactly("Arica", "Camarones", "General Lagos", "Putre");
    }

    @Test
    void onlyActiveServicesAreOffered() {
        assertThat(services.findByActiveTrueOrderByNameAsc()).hasSize(7);
        assertThat(services.findByIdInAndActiveTrue(List.of(1L, 4L, 999L)))
                .extracting(TextileService::getId).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    void ageRangesAndGendersKeepTheirOrder() {
        assertThat(ageRanges.findAllByOrderBySortOrderAsc()).extracting(AgeRange::getDescription)
                .startsWith("Menos de 18 años", "18-24").endsWith("Más de 65 años");
        assertThat(genders.findAllByOrderByIdAsc()).extracting(Gender::getDescription)
                .containsExactly("Femenino", "Masculino", "No binario", "Prefiero no responder");
    }
}
