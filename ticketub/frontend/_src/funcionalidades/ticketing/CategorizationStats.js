import React, { useState, useEffect } from 'react';
import { getCategorizationStats } from '../../logica_do_sistema/services/categorizationService';
import './CategorizationStats.css';

const CategorizationStats = ({ refreshTrigger }) => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let ativo = true;

    const fetchStats = async (silencioso = false) => {
      try {
        if (!silencioso) setLoading(true);
        const data = await getCategorizationStats();
        if (ativo) {
          setStats(data);
          setError(null);
        }
      } catch (err) {
        if (!silencioso && ativo) setError('Erro ao carregar estatísticas.');
      } finally {
        if (ativo && !silencioso) setLoading(false);
      }
    };

    fetchStats(false);

    const intervalId = setInterval(() => { if (ativo) fetchStats(true); }, 3000);
    return () => { ativo = false; clearInterval(intervalId); };
  }, [refreshTrigger]);

  if (loading) return <div className="stats-loading">A carregar estatísticas...</div>;
  if (error)   return <div className="stats-error">{error}</div>;
  if (!stats)  return null;

  return (
    <div className="stats-container">
      <div className="stats-grid">
        <div className="stat-card border-top-blue">
          <div className="stat-header" title="Total Classificados">Total Classificados</div>
          <div className="stat-value">{stats.totalClassificados}</div>
        </div>
        <div className="stat-card border-top-gray">
          <div className="stat-header" title="Total Não Categorizado">Total Não Categorizado</div>
          <div className="stat-value">{stats.totalNaoCategorizado}</div>
        </div>
        <div className="stat-card border-top-green">
          <div className="stat-header" title="Estudante">Estudante</div>
          <div className="stat-value">{stats.estudante}</div>
        </div>
        <div className="stat-card border-top-purple">
          <div className="stat-header" title="Sénior">Sénior</div>
          <div className="stat-value">{stats.senior}</div>
        </div>
        <div className="stat-card border-top-orange">
          <div className="stat-header" title="Normal">Normal</div>
          <div className="stat-value">{stats.normal}</div>
        </div>
      </div>
    </div>
  );
};

export default CategorizationStats;


