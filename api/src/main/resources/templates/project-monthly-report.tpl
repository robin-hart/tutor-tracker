\documentclass[11pt]{article}
\usepackage[margin=1in]{geometry}
\usepackage[T1]{fontenc}
\usepackage{booktabs}
\usepackage{longtable}

\begin{document}

\begin{center}
{\LARGE Monthly Project Report}\\[0.6em]
{\large {{PROJECT_NAME}}}\\
Project ID: {{PROJECT_ID}}\\
Month: {{MONTH_LABEL}}
\end{center}

\vspace{1em}
\noindent Generated at: {{GENERATED_AT}}

\vspace{1em}
\begin{tabular}{ll}
\textbf{Sessions} & {{SESSION_COUNT}} \\
\textbf{Total Hours} & {{TOTAL_HOURS}} \\
\textbf{Hourly Rate} & ${{HOURLY_RATE}}$ \\
\textbf{Gross Amount} & ${{GROSS_AMOUNT}}$ \\
\end{tabular}

\vspace{1.2em}
\noindent\textbf{Timeslots in Selected Month}

\begin{longtable}{p{0.45\linewidth}p{0.18\linewidth}p{0.14\linewidth}p{0.14\linewidth}}
\toprule
\textbf{Title} & \textbf{Date} & \textbf{Start} & \textbf{Minutes} \\
\midrule
\endhead
{{TIMESLOT_ROWS}}
\\
\bottomrule
\end{longtable}

\end{document}
