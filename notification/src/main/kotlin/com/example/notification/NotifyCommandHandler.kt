package com.example.notification

import com.example.cqrs_command.FailedToNotify
import com.example.cqrs_command.NotifyUserCommand
import com.example.cqrs_command.UserNotified
import io.eventuate.tram.commands.consumer.CommandDispatcher
import io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder
import io.eventuate.tram.commands.consumer.CommandHandlers
import io.eventuate.tram.commands.consumer.CommandMessage
import io.eventuate.tram.messaging.common.Message
import io.eventuate.tram.sagas.participant.SagaCommandDispatcherFactory
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration
import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration
import io.eventuate.tram.spring.optimisticlocking.OptimisticLockingDecoratorConfiguration
import org.apache.juli.logging.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl


class NotifyCommandHandler(private val javaMailSender: JavaMailSender) {

    private val logger = LogFactory.getLog(NotifyCommandHandler::class.java)

    fun commandHandlerDefinitions(): CommandHandlers = SagaCommandHandlersBuilder
        .fromChannel("notificationService")
        .onMessage(NotifyUserCommand::class.java, this::notify)
        .build()

    private fun notify(commandMessage: CommandMessage<NotifyUserCommand>): Message? {
        logger.info("Try to notify user: ${commandMessage.command.username}")
        return try {
            val message = SimpleMailMessage()
            message.setFrom("noreply@baeldung.com")
            message.setTo(commandMessage.command.username)
            message.setSubject("Welcome")
            message.setText("Hello from Spring microservice")
            javaMailSender.send(message)
            logger.info("Successfully notified")
            CommandHandlerReplyBuilder.withSuccess(UserNotified())
        } catch (e: Exception) {
            logger.warn("Failed to notify $e")
            CommandHandlerReplyBuilder.withFailure(FailedToNotify())
        }
    }
}


@Configuration
@Import(
    SagaParticipantConfiguration::class,
    OptimisticLockingDecoratorConfiguration::class,
    EventuateTramKafkaMessageConsumerConfiguration::class,
    TramMessageProducerJdbcConfiguration::class
)
class NotificationConfiguration {

    @Bean
    fun javaMailSender() = JavaMailSenderImpl()

    @Bean
    fun notifyCommandHandler(javaMailSender: JavaMailSender): NotifyCommandHandler =
        NotifyCommandHandler(javaMailSender)

    @Bean
    fun consumerCommandDispatcher(
        notifyCommandHandler: NotifyCommandHandler,
        sagaCommandDispatcherFactory: SagaCommandDispatcherFactory
    ): CommandDispatcher =
        sagaCommandDispatcherFactory.make(
            "com.example.order.saga.AccountSaga-consumer",
            notifyCommandHandler.commandHandlerDefinitions()
        )
}