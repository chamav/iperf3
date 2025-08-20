/* config.h for Android build */

#define PACKAGE_VERSION "3.16"
#define PACKAGE_BUGREPORT ""
#define PACKAGE_NAME "iperf3"
#define PACKAGE_STRING "iperf3 3.16"

/* Android specific defines */
#define HAVE_STDINT_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_SYS_SOCKET_H 1
#define HAVE_NETINET_IN_H 1
#define HAVE_ARPA_INET_H 1
#define HAVE_NETDB_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_STRING_H 1
#define HAVE_STDLIB_H 1
#define HAVE_UNISTD_H 1
#define HAVE_SIGNAL_H 1
#define HAVE_PTHREAD 1
#define HAVE_SCHED_H 1
#define HAVE_STRUCT_SOCKADDR_IN6 1
#define HAVE_POLL_H 1
#define HAVE_STDATOMIC_H 0

/* Functions available */
#define HAVE_SNPRINTF 1
#define HAVE_INET_NTOP 1
#define HAVE_INET_PTON 1
#define HAVE_GETTIMEOFDAY 1
#define HAVE_USLEEP 1
#define HAVE_CLOCK_GETTIME 1
#define HAVE_SCHED_SETAFFINITY 1
#define HAVE_CPU_SET_T 1

/* Disable features not available on Android */
#undef HAVE_FLOWLABEL
#undef HAVE_SCTP
#undef HAVE_SCTP_H
#undef HAVE_ENDPWENT
#undef HAVE_GETPASS

/* Set temp directory for Android */
#define IPERF_TEMP_DIR "/data/local/tmp"

/* TCP congestion control */
#define HAVE_TCP_CONGESTION 1

/* Don't use deprecated OpenSSL */
#undef HAVE_SSL

/* JSON support */
#define HAVE_JSON 1

/* Endianness */
#define WORDS_BIGENDIAN 0

/* Sizes */
#define SIZEOF_INT 4
#define SIZEOF_LONG 8
#define SIZEOF_SHORT 2