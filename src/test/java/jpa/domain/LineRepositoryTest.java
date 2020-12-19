package jpa.domain;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import jpa.domain.entity.Line;
import jpa.domain.entity.LineStation;
import jpa.domain.entity.Station;
import jpa.domain.repository.LineRepository;
import jpa.domain.repository.StationRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LineRepositoryTest {
	public static final String EXAMPLE_LINE_1 = "1호선";
	public static final String EXAMPLE_LINE_2 = "2호선";
	public static final String EXAMPLE_RED = "RED";
	public static final String EXAMPLE_GREEN = "GREEN";
	public static final String STATION_NAME1 = "신길역";
	public static final String STATION_NAME2 = "신도림역";
	public static final String STATION_NAME3 = "잠실역";

	@Autowired
	private LineRepository lineRepository;

	@Autowired
	private StationRepository stationRepository;

	@BeforeEach
	void setup() {
		Station station1 = stationRepository.save(Station.create(STATION_NAME1));
		Station station2 = stationRepository.save(Station.create(STATION_NAME2));
		Station station3 = stationRepository.save(Station.create(STATION_NAME3));

		Line line1 = Line.create(EXAMPLE_GREEN, EXAMPLE_LINE_1);
		Line line2 = Line.create(EXAMPLE_RED, EXAMPLE_LINE_2);
		line1.addLineStation(station1, station2, 10);
		line1.addLineStation(station2, station1, 10);
		line2.addLineStation(station3, null, null);

		lineRepository.save(line1);
		lineRepository.save(line2);

	}

	@DisplayName("단일 조회 테스트")
	@Test
	void findByName() {
		String expected = EXAMPLE_LINE_1;

		String actual = lineRepository.findByName(expected).getName();

		assertThat(actual).isEqualTo(expected);
	}

	@DisplayName("전체 조회 테스트")
	@Test
	void findAll() {
		int expectedLength = 2;

		List<Line> actualAll = lineRepository.findAll();
		List<String> nameAll = actualAll.stream().map(Line::getName).collect(Collectors.toList());

		assertAll(
			() -> assertThat(actualAll).hasSize(expectedLength),
			() -> assertThat(nameAll).contains(EXAMPLE_LINE_1, EXAMPLE_LINE_2)
		);

	}

	@Test
	@DisplayName("insert 테스트")
	void insert() {
		Line expected = Line.create("CYAN", "3호선");

		Line actual = lineRepository.save(expected);
		assertAll(
			() -> assertThat(actual.getId()).isNotNull(),
			() -> assertThat(actual.getName()).isEqualTo(expected.getName()),
			() -> assertThat(actual.getColor()).isEqualTo(expected.getColor())
		);
	}

	@Test
	@DisplayName("동일한 이름이 insert 되면 DataIntegrityViolationException이 발생한다.")
	void insertDuplicateName() {
		Line newLine = Line.create("CYAN", EXAMPLE_LINE_1);

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> {
				lineRepository.save(newLine);
			});
	}

	@Test
	@DisplayName("update 테스트")
	void update() {
		String expected = EXAMPLE_LINE_1;

		Line line = lineRepository.findByName(EXAMPLE_LINE_1);
		line.updateName(expected);
		Line check = lineRepository.findByName(expected);

		assertAll(
			() -> assertThat(check.getId()).isEqualTo(line.getId()),
			() -> assertThat(check.getName()).isEqualTo(line.getName())
		);
	}

	@Test
	@DisplayName("delete 테스트")
	void delete() {
		Line line = lineRepository.findByName(EXAMPLE_LINE_1);
		lineRepository.delete(line);
		Line check = lineRepository.findByName(EXAMPLE_LINE_1);

		assertThat(check).isNull();
	}

	@Test
	@DisplayName("노선 조회 시 속한 지하철역을 볼 수 있다.")
	void lineStation() {

		Line line = lineRepository.findByName(EXAMPLE_LINE_1);
		List<LineStation> lineStations = line.getLineStations();
		List<Station> stations = CollectionUtils.emptyIfNull(lineStations).stream()
			.map(LineStation::getStation)
			.collect(Collectors.toList());
		List<String> stationNames = CollectionUtils.emptyIfNull(stations).stream()
			.map(Station::getName)
			.collect(Collectors.toList());

		assertAll(
			() -> assertThat(stations).hasSize(2),
			() -> assertThat(stationNames).contains(STATION_NAME1, STATION_NAME2)
		);
	}
}