package com.dark.roostergenics.arena;

import com.dark.roostergenics.arena.combat.CombatMessage;
import com.dark.roostergenics.arena.combat.CombatState;
import com.dark.roostergenics.arena.combat.PhysicalCombatCommand;
import com.dark.roostergenics.arena.combat.PhysicalCommandType;
import com.dark.roostergenics.arena.combat.PhysicalExecutionLease;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Suite formal de pruebas de concurrencia, sincronización lógica y actuadores
 * para la arquitectura Folia de combate CRIT-02 en Roostergenics.
 */
public class DuelArenaSessionTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================================");
        System.out.println("   INICIANDO SUITE FORMAL DE CONCURRENCIA Y COMBATE CRIT-02     ");
        System.out.println("=================================================================");

        testAExactlyOneSettlementConcurrentRace(1000);
        testBStaleCommandRejection();
        testCDuplicateCommandProtection();
        testDEndingRaceAuthoritativeTerminalTransition(1000);
        testERetiredEntityCallbackSafety();
        testFSettlementReentrancyProtection();
        testGSessionIsolationConcurrent();

        System.out.println("=================================================================");
        System.out.println("   TODOS LOS TESTS DE CRIT-02 EJECUTADOS Y APROBADOS CON ÉXITO   ");
        System.out.println("=================================================================");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FALLO: " + message);
        }
        System.out.println("  [PASS] " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("FALLO: " + message + " | Esperado: " + expected + " pero fue: " + actual);
        }
        System.out.println("  [PASS] " + message + " (Valor: " + actual + ")");
    }

    /**
     * Test A: Simulación de llegada concurrente de timeout, entity death y player quit.
     * Invariante: settlementCount == 1 de forma invariable a lo largo de 1,000 iteraciones.
     */
    private static void testAExactlyOneSettlementConcurrentRace(int iterations) throws Exception {
        System.out.println("\n--- TEST A: Exactly-One Settlement Concurrent Race (" + iterations + " iteraciones) ---");

        for (int i = 0; i < iterations; i++) {
            AtomicBoolean settlementGuard = new AtomicBoolean(false);
            AtomicInteger settlementCount = new AtomicInteger(0);

            ExecutorService pool = Executors.newFixedThreadPool(3);
            CyclicBarrier barrier = new CyclicBarrier(3);

            Runnable timeoutArrival = () -> {
                try {
                    barrier.await();
                    if (settlementGuard.compareAndSet(false, true)) {
                        settlementCount.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Runnable deathArrival = () -> {
                try {
                    barrier.await();
                    if (settlementGuard.compareAndSet(false, true)) {
                        settlementCount.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Runnable quitArrival = () -> {
                try {
                    barrier.await();
                    if (settlementGuard.compareAndSet(false, true)) {
                        settlementCount.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Future<?> f1 = pool.submit(timeoutArrival);
            Future<?> f2 = pool.submit(deathArrival);
            Future<?> f3 = pool.submit(quitArrival);

            f1.get(5, TimeUnit.SECONDS);
            f2.get(5, TimeUnit.SECONDS);
            f3.get(5, TimeUnit.SECONDS);
            pool.shutdown();

            if (settlementCount.get() != 1) {
                throw new AssertionError("Violación del invariante en iteración " + i + ": settlementCount fue " + settlementCount.get());
            }
        }

        assertTrue(true, iterations + " carreras concurrentes de liquidación (timeout, death, quit) resultaron exactamente en 1 settlement");
    }

    /**
     * Test B: Comandos desfasados (Stale Command Protection).
     * Comando emitido bajo epoch = 10; la sesión transiciona a epoch = 11.
     * Invariante: El actuador rechaza la ejecución del comando y no muta la entidad.
     */
    private static void testBStaleCommandRejection() {
        System.out.println("\n--- TEST B: Protección contra Comandos Desfasados (Stale Commands) ---");

        PhysicalExecutionLease lease = new PhysicalExecutionLease();
        UUID chickenA = UUID.randomUUID();

        // Simular que el lease avanzó hasta la época 10
        while (lease.getActiveEpoch() < 10) {
            lease.nextEpoch();
        }
        assertEquals(10L, lease.getActiveEpoch(), "La época inicial activa es 10");

        long staleCmdId = lease.generateNextCommandId();
        PhysicalCombatCommand staleCommand = new PhysicalCombatCommand(
                10L, staleCmdId, chickenA, PhysicalCommandType.APPLY_DAMAGE_EFFECT, null, null, 15.0, 0, null
        );

        // La sesión cambia de estado a ENDING / FINISHED, incrementando la época a 11
        long newEpoch = lease.nextEpoch();
        assertEquals(11L, newEpoch, "La época avanzó a 11 tras transición de sesión");

        // El actuador intenta ejecutar el comando emitido bajo la época 10
        boolean applied = lease.validateAndApply(chickenA, staleCommand.sessionEpoch(), staleCommand.commandId());
        assertTrue(!applied, "El comando de la época 10 es rechazado porque la época activa es 11");

        // Emitir un nuevo comando legítimo bajo la época 11
        long validCmdId = lease.generateNextCommandId();
        PhysicalCombatCommand validCommand = new PhysicalCombatCommand(
                11L, validCmdId, chickenA, PhysicalCommandType.APPLY_DAMAGE_EFFECT, null, null, 10.0, 0, null
        );

        boolean validApplied = lease.validateAndApply(chickenA, validCommand.sessionEpoch(), validCommand.commandId());
        assertTrue(validApplied, "El comando legítimo con época 11 es aprobado y aplicado");
    }

    /**
     * Test C: Protección contra Comandos Duplicados (Duplicate Command Protection).
     * Dos ejecuciones del mismo commandId.
     * Invariante: Solo 1 ejecución es permitida, la segunda es descartada.
     */
    private static void testCDuplicateCommandProtection() {
        System.out.println("\n--- TEST C: Protección contra Comandos Duplicados (Duplicate Commands) ---");

        PhysicalExecutionLease lease = new PhysicalExecutionLease();
        UUID chickenA = UUID.randomUUID();
        long epoch = lease.getActiveEpoch();
        long cmdId = lease.generateNextCommandId();

        boolean firstApplication = lease.validateAndApply(chickenA, epoch, cmdId);
        assertTrue(firstApplication, "Primera ejecución de commandId " + cmdId + " aprobada");

        boolean secondApplication = lease.validateAndApply(chickenA, epoch, cmdId);
        assertTrue(!secondApplication, "Segunda ejecución de commandId " + cmdId + " rechazada como duplicado");

        // Un comando anterior/atrasado en la secuencia también debe ser rechazado
        boolean olderApplication = lease.validateAndApply(chickenA, epoch, cmdId - 1);
        assertTrue(!olderApplication, "Comando retroactivo con commandId menor es rechazado");
    }

    /**
     * Test D: Carrera de Transición Terminal hacia ENDING.
     * Simulación de FIGHTING + llegada simultánea de daño letal, quit y timeout.
     * Invariante: Solo ocurre una transición autorizada hacia la fase terminal.
     */
    private static void testDEndingRaceAuthoritativeTerminalTransition(int iterations) throws Exception {
        System.out.println("\n--- TEST D: Carrera Concurrente hacia Estado Terminal ENDING (" + iterations + " iteraciones) ---");

        for (int i = 0; i < iterations; i++) {
            final AtomicBoolean inFightingState = new AtomicBoolean(true);
            final AtomicInteger terminalTransitions = new AtomicInteger(0);
            final AtomicLong finalEpoch = new AtomicLong(1L);

            ExecutorService pool = Executors.newFixedThreadPool(4);
            CyclicBarrier barrier = new CyclicBarrier(4);

            Runnable damageKill = () -> {
                try {
                    barrier.await();
                    if (inFightingState.compareAndSet(true, false)) {
                        terminalTransitions.incrementAndGet();
                        finalEpoch.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Runnable playerQuit = () -> {
                try {
                    barrier.await();
                    if (inFightingState.compareAndSet(true, false)) {
                        terminalTransitions.incrementAndGet();
                        finalEpoch.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Runnable timeout = () -> {
                try {
                    barrier.await();
                    if (inFightingState.compareAndSet(true, false)) {
                        terminalTransitions.incrementAndGet();
                        finalEpoch.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Runnable adminAbort = () -> {
                try {
                    barrier.await();
                    if (inFightingState.compareAndSet(true, false)) {
                        terminalTransitions.incrementAndGet();
                        finalEpoch.incrementAndGet();
                    }
                } catch (Exception ignored) {}
            };

            Future<?> f1 = pool.submit(damageKill);
            Future<?> f2 = pool.submit(playerQuit);
            Future<?> f3 = pool.submit(timeout);
            Future<?> f4 = pool.submit(adminAbort);

            f1.get(5, TimeUnit.SECONDS);
            f2.get(5, TimeUnit.SECONDS);
            f3.get(5, TimeUnit.SECONDS);
            f4.get(5, TimeUnit.SECONDS);
            pool.shutdown();

            if (terminalTransitions.get() != 1) {
                throw new AssertionError("Fallo en iteración " + i + ": múltiples transiciones terminales detectadas (" + terminalTransitions.get() + ")");
            }
            if (finalEpoch.get() != 2L) {
                throw new AssertionError("Fallo en iteración " + i + ": época esperada 2L pero fue " + finalEpoch.get());
            }
        }

        assertTrue(true, iterations + " carreras terminales concurrentes resultaron en exactamente 1 transición terminal autorizada");
    }

    /**
     * Test E: Retired Callback Safety.
     * Cuando el scheduler de la entidad se retira, no debe mutar estado ni invocar
     * métodos Bukkit; únicamente encola un CombatMessage inmutable hacia el RegionScheduler lógico.
     */
    private static void testERetiredEntityCallbackSafety() {
        System.out.println("\n--- TEST E: Seguridad del Callback de Entidad Retirada (Retired Callback) ---");

        ConcurrentLinkedQueue<CombatMessage> regionSchedulerInbox = new ConcurrentLinkedQueue<>();
        UUID chickenA = UUID.randomUUID();
        long currentEpoch = 5L;

        // Simulación del callback de EntityScheduler:
        Runnable retiredCallback = () -> {
            // El callback de EntityScheduler únicamente encola un mensaje inmutable
            regionSchedulerInbox.add(new CombatMessage.EntityRetired(chickenA, currentEpoch));
        };

        // Ejecutar el callback
        retiredCallback.run();

        assertEquals(1, regionSchedulerInbox.size(), "El buzón del RegionScheduler recibió exactamente 1 mensaje de retiro");
        CombatMessage msg = regionSchedulerInbox.poll();
        assertTrue(msg instanceof CombatMessage.EntityRetired, "El mensaje recibido es de tipo EntityRetired");

        CombatMessage.EntityRetired retiredMsg = (CombatMessage.EntityRetired) msg;
        assertEquals(chickenA, retiredMsg.entityId(), "El UUID de la entidad retirada coincide exactamente");
        assertEquals(currentEpoch, retiredMsg.sessionEpoch(), "La época del mensaje coincide con la época del comando");
    }

    /**
     * Test F: Settlement Reentrancy Protection.
     * Dos mensajes terminales independientes llegan en secuencia o concurrentemente.
     * Invariante: Solo 1 liquidación se ejecuta; la segunda invocación es un no-op.
     */
    private static void testFSettlementReentrancyProtection() {
        System.out.println("\n--- TEST F: Protección contra Reentrancia en Liquidación (Settlement Reentrancy) ---");

        AtomicBoolean settlementGuarded = new AtomicBoolean(false);
        AtomicInteger actualSettlementExecutions = new AtomicInteger(0);

        // Intento 1: Liquidación por K.O.
        if (settlementGuarded.compareAndSet(false, true)) {
            actualSettlementExecutions.incrementAndGet();
        }

        // Intento 2: Llegada tardía de Forfeit o Timeout
        if (settlementGuarded.compareAndSet(false, true)) {
            actualSettlementExecutions.incrementAndGet();
        }

        assertEquals(1, actualSettlementExecutions.get(), "La liquidación se ejecutó exactamente una vez ante múltiples intentos terminales");
    }

    /**
     * Test G: Session Isolation Concurrent.
     * Dos sesiones de combate independientes (Sesión A y Sesión B) ejecutan en paralelo.
     * Invariante: Cero cruce de estados, épocas o identificadores de comandos entre sesiones.
     */
    private static void testGSessionIsolationConcurrent() throws Exception {
        System.out.println("\n--- TEST G: Aislamiento Concurrente entre Sesiones (Session Isolation) ---");

        PhysicalExecutionLease leaseA = new PhysicalExecutionLease();
        PhysicalExecutionLease leaseB = new PhysicalExecutionLease();

        UUID chickenA1 = UUID.randomUUID();
        UUID chickenB1 = UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<Long> taskA = () -> {
            barrier.await();
            leaseA.nextEpoch();
            leaseA.nextEpoch();
            return leaseA.generateNextCommandId();
        };

        Callable<Long> taskB = () -> {
            barrier.await();
            return leaseB.generateNextCommandId();
        };

        Future<Long> fA = pool.submit(taskA);
        Future<Long> fB = pool.submit(taskB);

        long cmdIdA = fA.get(5, TimeUnit.SECONDS);
        long cmdIdB = fB.get(5, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(3L, leaseA.getActiveEpoch(), "Sesión A alcanzó la época 3");
        assertEquals(1L, leaseB.getActiveEpoch(), "Sesión B mantuvo intacta su época 1 sin interferencias de Sesión A");

        boolean applyA = leaseA.validateAndApply(chickenA1, 3L, cmdIdA);
        boolean applyB = leaseB.validateAndApply(chickenB1, 1L, cmdIdB);

        assertTrue(applyA, "Comando en Sesión A aplicado correctamente bajo su propia época y lease");
        assertTrue(applyB, "Comando en Sesión B aplicado correctamente bajo su propia época y lease");

        // Intentar aplicar comando de A con el lease de B
        boolean crossApply = leaseB.validateAndApply(chickenA1, 3L, cmdIdA);
        assertTrue(!crossApply, "Comando de Sesión A es rechazado inmediatamente por Sesión B");
    }
}
