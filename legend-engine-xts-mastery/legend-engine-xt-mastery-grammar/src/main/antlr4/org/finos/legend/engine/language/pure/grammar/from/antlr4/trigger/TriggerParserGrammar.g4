parser grammar TriggerParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = TriggerLexerGrammar;
}

identifier:                      VALID_STRING | STRING | CRON | MANUAL
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (
                                                cronTrigger
                                            )
                                            EOF
;

// -------------------------------------- CRON TRIGGER --------------------------------------
cronTrigger:                        (
                                        minute
                                        | hour
                                        | days
                                        | month
                                        | dayOfMonth
                                        | year
                                        | timezone
                                        | frequency
                                    )*
                                    EOF
;

minute:                             MINUTE COLON INTEGER SEMI_COLON
;
hour:                               HOUR COLON INTEGER SEMI_COLON
;
days:                               DAYS COLON
                                        BRACKET_OPEN
                                       (
                                           dayValue
                                            (
                                                COMMA
                                                dayValue
                                            )*
                                        )
                                        BRACKET_CLOSE SEMI_COLON
;
month:                              MONTH COLON monthValue SEMI_COLON
;
dayOfMonth:                         DAY_OF_MONTH COLON INTEGER SEMI_COLON
;
year:                               YEAR COLON INTEGER SEMI_COLON
;
timezone:                           TIME_ZONE COLON STRING SEMI_COLON
;
frequency:                          FREQUENCY COLON frequencyValue SEMI_COLON
;
dayValue:                                MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY
;
frequencyValue:                     DAILY | WEEKLY | INTRA_DAY
;
monthValue:                         JANUARY | FEBRUARY | MARCH | APRIL | MAY | JUNE | JULY | AUGUST | SEPTEMBER | OCTOBER
                                    | NOVEMBER | DECEMBER
;