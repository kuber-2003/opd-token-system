package com.medoc.opd;

import com.medoc.opd.model.*;
import com.medoc.opd.service.OPDTokenEngine;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Simulation of a complete OPD day with 3 doctors.
 * Demonstrates all features of the token allocation system.
 */
@Slf4j
public class OPDSimulation {

        private static final LocalDate TODAY = LocalDate.now();

        public static void main(String[] args) {
                OPDTokenEngine engine = new OPDTokenEngine();
                engine.setSimulationMode(true);

                printHeader("OPD TOKEN ALLOCATION SYSTEM - DAY SIMULATION");

                // Phase 1: Setup Doctor Slots
                printPhase("PHASE 1: Creating Time Slots for 3 Doctors");
                setupDoctorSlots(engine);

                // Phase 2: Online Bookings (Morning)
                printPhase("PHASE 2: Processing Online Bookings");
                processOnlineBookings(engine);

                // Phase 3: Walk-in Patients
                printPhase("PHASE 3: Walk-in Registrations");
                processWalkIns(engine);

                // Phase 4: Paid Priority Patients
                printPhase("PHASE 4: Priority Patient Allocations");
                processPriorityPatients(engine);

                // Phase 5: Emergency Case
                printPhase("PHASE 5: EMERGENCY ADMISSION with Automatic Reallocation");
                processEmergency(engine);

                // Phase 6: Cancellations
                printPhase("PHASE 6: Handling Patient Cancellations");
                processCancellations(engine);

                // Phase 7: Check-ins
                printPhase("PHASE 7: Patient Check-ins (Dynamic Priority)");
                processCheckIns(engine);

                // Phase 8: No-shows
                printPhase("PHASE 8: Detecting and Managing No-shows");
                processNoShows(engine);

                // Phase 9: Consultations
                printPhase("PHASE 9: Starting and Completing Consultations");
                processConsultations(engine);

                // Phase 10: Dynamic Capacity Adjustment
                printPhase("PHASE 10: Dynamic Capacity Adjustment (Doctor Running Late)");
                adjustCapacity(engine);

                // Phase 11: Follow-up Patients
                printPhase("PHASE 11: Follow-up Patient Allocations");
                processFollowUps(engine);

                // Phase 12: Queue Status
                printPhase("PHASE 12: Current Queue Status by Doctor");
                displayQueues(engine);

                // Phase 13: Statistics
                printPhase("PHASE 13: Final Statistics and Analysis");
                displayStatistics(engine);

                printHeader("SIMULATION COMPLETED SUCCESSFULLY");
        }

        private static void setupDoctorSlots(OPDTokenEngine engine) {
                // Dr. Sharma - Cardiology (3 slots)
                engine.createSlot("DR001", "Dr. Rajesh Sharma", "Cardiology",
                                dateTime(9, 0), dateTime(10, 0), 8);
                engine.createSlot("DR001", "Dr. Rajesh Sharma", "Cardiology",
                                dateTime(10, 0), dateTime(11, 0), 10);
                engine.createSlot("DR001", "Dr. Rajesh Sharma", "Cardiology",
                                dateTime(11, 0), dateTime(12, 0), 8);

                // Dr. Verma - Orthopedics (3 slots)
                engine.createSlot("DR002", "Dr. Priya Verma", "Orthopedics",
                                dateTime(9, 0), dateTime(10, 0), 6);
                engine.createSlot("DR002", "Dr. Priya Verma", "Orthopedics",
                                dateTime(10, 0), dateTime(11, 0), 8);
                engine.createSlot("DR002", "Dr. Priya Verma", "Orthopedics",
                                dateTime(11, 0), dateTime(12, 0), 6);

                // Dr. Kumar - General Medicine (3 slots)
                engine.createSlot("DR003", "Dr. Amit Kumar", "General Medicine",
                                dateTime(9, 0), dateTime(10, 0), 12);
                engine.createSlot("DR003", "Dr. Amit Kumar", "General Medicine",
                                dateTime(10, 0), dateTime(11, 0), 15);
                engine.createSlot("DR003", "Dr. Amit Kumar", "General Medicine",
                                dateTime(11, 0), dateTime(12, 0), 12);

                log.info("✓ Created 9 time slots across 3 doctors");
        }

        private static void processOnlineBookings(OPDTokenEngine engine) {
                // Online bookings made in advance
                engine.allocateToken("PAT001", "Ramesh Gupta", "DR001",
                                TokenSource.ONLINE_BOOKING, dateTime(9, 15),
                                "Regular checkup");

                engine.allocateToken("PAT002", "Sunita Devi", "DR001",
                                TokenSource.ONLINE_BOOKING, dateTime(9, 30),
                                "Follow-up on medication");

                engine.allocateToken("PAT003", "Arjun Singh", "DR002",
                                TokenSource.ONLINE_BOOKING, dateTime(9, 0),
                                "Knee pain evaluation");

                engine.allocateToken("PAT004", "Meera Sharma", "DR003",
                                TokenSource.ONLINE_BOOKING, dateTime(10, 0),
                                "Fever and cough");

                engine.allocateToken("PAT005", "Vikram Reddy", "DR003",
                                TokenSource.ONLINE_BOOKING, dateTime(10, 30),
                                "Diabetes checkup");

                log.info("✓ Processed 5 online bookings");
        }

        private static void processWalkIns(OPDTokenEngine engine) {
                // Walk-in patients arriving at OPD desk
                engine.allocateToken("PAT006", "Lakshmi Nair", "DR001",
                                TokenSource.WALK_IN, dateTime(9, 0),
                                "Chest discomfort");

                engine.allocateToken("PAT007", "Rajiv Malhotra", "DR002",
                                TokenSource.WALK_IN, dateTime(9, 15),
                                "Back pain");

                engine.allocateToken("PAT008", "Anita Patel", "DR003",
                                TokenSource.WALK_IN, dateTime(9, 0),
                                "General consultation");

                log.info("✓ Registered 3 walk-in patients");
        }

        private static void processPriorityPatients(OPDTokenEngine engine) {
                // Paid priority service patients
                engine.allocateToken("PAT009", "Suresh Kapoor (VIP)", "DR001",
                                TokenSource.PAID_PRIORITY, dateTime(9, 0),
                                "Premium health screening");

                engine.allocateToken("PAT010", "Divya Menon (VIP)", "DR003",
                                TokenSource.PAID_PRIORITY, dateTime(9, 0),
                                "Executive health checkup");

                log.info("✓ Allocated 2 priority tokens");
        }

        private static void processEmergency(OPDTokenEngine engine) {
                // Emergency patient arrives - should trigger reallocation
                log.warn("⚠ EMERGENCY: Patient with acute chest pain");

                Token emergencyToken = engine.allocateEmergencyToken(
                                "PAT_EMG_001", "Mohan Das (EMERGENCY)", "DR001",
                                "SEVERE CHEST PAIN - Possible MI");

                log.warn("✓ Emergency token {} allocated to Dr. Sharma's current slot",
                                emergencyToken.getTokenNumber());
                log.info("  System automatically reallocated lower priority tokens to next slots");
        }

        private static void processCancellations(OPDTokenEngine engine) {
                // Some patients cancel their appointments
                List<Token> allTokens = engine.getAllTokens();

                if (allTokens.size() >= 2) {
                        Token token1 = allTokens.get(1);
                        engine.cancelToken(token1.getTokenId());
                        log.info("✓ Patient {} cancelled appointment", token1.getPatientName());
                }

                log.info("✓ Processed cancellations, capacity freed up");
        }

        private static void processCheckIns(OPDTokenEngine engine) {
                // Patients arrive and check in
                List<Token> tokens = engine.getAllTokens().stream()
                                .filter(t -> t.getStatus() == TokenStatus.ALLOCATED
                                                || t.getStatus() == TokenStatus.REALLOCATED)
                                .limit(6)
                                .toList();

                for (Token token : tokens) {
                        engine.checkIn(token.getTokenId());
                        log.info("✓ Token {} checked in - {} (Priority: {}, Source: {})",
                                        token.getTokenNumber(),
                                        token.getPatientName(),
                                        String.format("%.1f", token.getDynamicPriority()),
                                        token.getSource());
                }

                log.info("✓ {} patients checked in - dynamic priority now active", tokens.size());
        }

        private static void processNoShows(OPDTokenEngine engine) {
                // Some patients don't show up
                List<Token> allocatedTokens = engine.getAllTokens().stream()
                                .filter(t -> t.getStatus() == TokenStatus.ALLOCATED)
                                .limit(1)
                                .toList();

                for (Token token : allocatedTokens) {
                        engine.markNoShow(token.getTokenId());
                        log.info("✓ Token {} marked as NO-SHOW - {}", token.getTokenNumber(), token.getPatientName());
                }

                log.info("✓ No-shows processed, capacity recovered");
        }

        private static void processConsultations(OPDTokenEngine engine) {
                // Start and complete consultations
                List<Token> checkedInTokens = engine.getAllTokens().stream()
                                .filter(t -> t.getStatus() == TokenStatus.CHECKED_IN)
                                .sorted((a, b) -> Double.compare(b.getDynamicPriority(), a.getDynamicPriority()))
                                .limit(3)
                                .toList();

                for (Token token : checkedInTokens) {
                        engine.startConsultation(token.getTokenId());
                        log.info("✓ Consultation started - Token {} ({})",
                                        token.getTokenNumber(), token.getPatientName());

                        engine.completeConsultation(token.getTokenId());
                        log.info("  Consultation completed - Wait time: {} minutes",
                                        token.getWaitTimeMinutes());
                }

                log.info("✓ Completed {} consultations", checkedInTokens.size());
        }

        private static void adjustCapacity(OPDTokenEngine engine) {
                // Doctor running late, reduce capacity
                List<TimeSlot> slots = engine.getDoctorSlots("DR002");

                if (!slots.isEmpty()) {
                        TimeSlot slot = slots.get(1); // 10-11 AM slot
                        log.warn("⚠ Dr. Verma running late, reducing 10-11 slot capacity from {} to 5",
                                        slot.getMaxCapacity());

                        engine.adjustSlotCapacity(slot.getSlotId(), 5);
                        log.info("✓ Capacity adjusted, overflow tokens reallocated");
                }
        }

        private static void processFollowUps(OPDTokenEngine engine) {
                // Follow-up patients
                engine.allocateToken("PAT011", "Rohan Joshi (Follow-up)", "DR001",
                                TokenSource.FOLLOW_UP, dateTime(11, 0),
                                "Post-surgery checkup");

                engine.allocateToken("PAT012", "Kavita Iyer (Follow-up)", "DR003",
                                TokenSource.FOLLOW_UP, dateTime(11, 30),
                                "Blood pressure monitoring");

                log.info("✓ Allocated 2 follow-up tokens");
        }

        private static void displayQueues(OPDTokenEngine engine) {
                String[] doctors = { "DR001", "DR002", "DR003" };
                String[] names = { "Dr. Rajesh Sharma (Cardiology)", "Dr. Priya Verma (Orthopedics)",
                                "Dr. Amit Kumar (General Medicine)" };

                for (int i = 0; i < doctors.length; i++) {
                        List<Token> queue = engine.getDoctorQueue(doctors[i]);

                        log.info("\n{}", names[i]);
                        log.info("  Active patients in queue: {}", queue.size());

                        if (!queue.isEmpty()) {
                                log.info("  Priority Order:");
                                for (int j = 0; j < Math.min(5, queue.size()); j++) {
                                        Token t = queue.get(j);
                                        log.info("    {}. Token {} - {} (Priority: {}, Status: {})",
                                                        j + 1,
                                                        t.getTokenNumber(),
                                                        t.getPatientName(),
                                                        String.format("%.1f", t.getDynamicPriority()),
                                                        t.getStatus());
                                }
                        }
                }
        }

        private static void displayStatistics(OPDTokenEngine engine) {
                String[] doctors = { "DR001", "DR002", "DR003" };
                String[] names = { "Dr. Sharma", "Dr. Verma", "Dr. Kumar" };

                for (int i = 0; i < doctors.length; i++) {
                        Map<String, Object> stats = engine.getStatistics(doctors[i]);

                        log.info("\n{} Statistics:", names[i]);
                        log.info("  Total Tokens: {}", stats.get("totalTokens"));
                        log.info("  Active: {}", stats.get("activeTokens"));
                        log.info("  Completed: {}", stats.get("completedTokens"));
                        log.info("  Cancelled: {}", stats.get("cancelledTokens"));
                        log.info("  No-shows: {}", stats.get("noShowTokens"));
                        log.info("  Emergency: {}", stats.get("emergencyTokens"));
                        log.info("  Avg Utilization: {}%", String.format("%.1f", stats.get("averageUtilization")));
                }

                // Overall statistics
                Map<String, Object> overall = engine.getStatistics(null);
                log.info("\nOverall System Statistics:");
                log.info("  Total Tokens Generated: {}", overall.get("totalTokens"));
                log.info("  Active: {}", overall.get("activeTokens"));
                log.info("  Completed: {}", overall.get("completedTokens"));
                log.info("  Cancelled: {}", overall.get("cancelledTokens"));
                log.info("  No-shows: {}", overall.get("noShowTokens"));
                log.info("  Emergency Cases: {}", overall.get("emergencyTokens"));
        }

        // Helper methods
        private static LocalDateTime dateTime(int hour, int minute) {
                return LocalDateTime.of(TODAY, LocalTime.of(hour, minute));
        }

        private static void printHeader(String title) {
                System.out.println("\n" + "=".repeat(80));
                System.out.println(" ".repeat((80 - title.length()) / 2) + title);
                System.out.println("=".repeat(80) + "\n");
        }

        private static void printPhase(String phase) {
                System.out.println("\n" + "-".repeat(80));
                System.out.println(phase);
                System.out.println("-".repeat(80));
        }
}
