package com.resilience4j.lab.fault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class FaultSimulatorTest {

    private FaultSimulator faultSimulator;

    @BeforeEach
    void setUp() {
        faultSimulator = new FaultSimulator();
    }

    @Test
    @DisplayName("Should always throw when configured to always fail")
    void shouldAlwaysThrow() {
        faultSimulator.alwaysFail();

        assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                .isInstanceOf(SimulatedException.class)
                .hasMessageContaining("test");
    }

    @Test
    @DisplayName("Should keep throwing on every call when always fail is active")
    void shouldKeepThrowingOnEveryCall() {
        faultSimulator.alwaysFail();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                    .isInstanceOf(SimulatedException.class);
        }
    }

    @Test
    @DisplayName("Should throw exactly N times then succeed")
    void shouldThrowExactlyNTimesThenSucceed() {
        faultSimulator.failForFirstNCalls(3);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                    .isInstanceOf(SimulatedException.class);
        }

        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));
    }

    @Test
    @DisplayName("Should not throw on first call when N is zero")
    void shouldNotThrowWhenNIsZero() {
        faultSimulator.failForFirstNCalls(0);

        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));
    }

    @Test
    @DisplayName("Should fail exactly N times not N plus one")
    void shouldFailExactlyNTimesNotMore() {
        faultSimulator.failForFirstNCalls(2);

        assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                .isInstanceOf(SimulatedException.class)
                .hasMessageContaining("#1");

        assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                .isInstanceOf(SimulatedException.class)
                .hasMessageContaining("#2");

        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));

        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));
    }

    @Test
    @DisplayName("Should not throw after reset when always fail was active")
    void shouldNotThrowAfterResetFromAlwaysFail() {
        faultSimulator.alwaysFail();
        faultSimulator.reset();

        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));
    }

    @Test
    @DisplayName("Should reset call counter and not throw after reset")
    void shouldResetCallCounter() {
        faultSimulator.failForFirstNCalls(3);

        assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                .isInstanceOf(SimulatedException.class);
        assertThatThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"))
                .isInstanceOf(SimulatedException.class);

        faultSimulator.reset();

        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));
    }


    @Test
    @DisplayName("Should not throw by default without any configuration")
    void shouldNotThrowByDefault() {
        assertThatNoException()
                .isThrownBy(() -> faultSimulator.checkAndThrowIfNeeded("test"));
    }
}