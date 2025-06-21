package com.virtualcompanion.user.controller;

class LoadTest {

    @Autowired
    private ConversationService conversationService;

    @Test
    @DisplayName("Should handle concurrent message processing")
    void testConcurrentMessages() throws Exception {
        // Given
        int numberOfThreads = 10;
        int messagesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        // When
        IntStream.range(0, numberOfThreads).forEach(i -> {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerThread; j++) {
                        SendMessageRequest request = new SendMessageRequest();
                        request.setContent("Concurrent message " + j);
                        // Process message
                        conversationService.processMessage(request);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        });

        // Then
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
    }

    @Test
    @DisplayName("Should maintain response time under load")
    void testResponseTimeUnderLoad() throws Exception {
        // Given
        int requests = 1000;
        long maxResponseTime = 200; // milliseconds
        
        // When
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < requests; i++) {
            long start = System.currentTimeMillis();
            
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Performance test message");
            conversationService.processMessage(request);
            
            long responseTime = System.currentTimeMillis() - start;
            responseTimes.add(responseTime);
        }

        // Then
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
                
        long p95ResponseTime = responseTimes.stream()
                .sorted()
                .skip((long) (requests * 0.95))
                .findFirst()
                .orElse(0L);

        assertThat(avgResponseTime).isLessThan(maxResponseTime);
        assertThat(p95ResponseTime).isLessThan(maxResponseTime * 2);
    }
}
