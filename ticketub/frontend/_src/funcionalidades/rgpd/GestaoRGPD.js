import React, { useState, useEffect, useCallback } from 'react';
import { getPoliticas, createPolitica, updatePolitica, deletePolitica } from '../../logica_do_sistema/services/rgpdService';
import './GestaoRGPD.css';

const GestaoRGPD = () => {
  const [politicas, setPoliticas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    nome: '',
    tipoDado: 'BIOMETRICO',
    tempoRetencao: 30,
    descricao: ''
  });

  const fetchPoliticas = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPoliticas();
      setPoliticas(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro a carregar políticas RGPD', err);
      setPoliticas([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPoliticas();
  }, [fetchPoliticas]);

  const handleOpenModal = (politica = null) => {
    if (politica) {
      setFormData({
        nome: politica.nome,
        tipoDado: politica.tipoDado,
        tempoRetencao: politica.tempoRetencao,
        descricao: politica.descricao || ''
      });
      setEditingId(politica.id);
    } else {
      setFormData({
        nome: '',
        tipoDado: 'BIOMETRICO',
        tempoRetencao: 30,
        descricao: ''
      });
      setEditingId(null);
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await updatePolitica(editingId, formData);
      } else {
        await createPolitica(formData);
      }
      await fetchPoliticas();
      handleCloseModal();
    } catch (err) {
      console.error('Erro a guardar política', err);
    }
  };

  const handleDelete = async (id, nome) => {
    if (window.confirm(`Tem a certeza que deseja eliminar a política "${nome}"?`)) {
      try {
        await deletePolitica(id);
        await fetchPoliticas();
      } catch (err) {
        console.error('Erro a eliminar política', err);
      }
    }
  };

  return (
    <div className="rgpd-container">
      <div className="rgpd-header">
        <h2>Políticas de Anonimização (RGPD)</h2>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          Criar Nova Política
        </button>
      </div>

      <div className="rgpd-table-container">
        {loading ? (
          <div style={{ padding: '2rem', textAlign: 'center' }}>A carregar políticas...</div>
        ) : (
          <table className="rgpd-table">
            <thead>
              <tr>
                <th>Nome da Política</th>
                <th>Tipo de Dado</th>
                <th>Retenção (Dias)</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {politicas.length > 0 ? (
                politicas.map((p) => (
                  <tr key={p.id}>
                    <td style={{ fontWeight: 600 }}>{p.nome}</td>
                    <td>{p.tipoDado}</td>
                    <td>{p.tempoRetencao}</td>
                    <td className="actions-cell">
                      <button className="btn-icon-edit" onClick={() => handleOpenModal(p)}>Editar</button>
                      <button className="btn-icon-delete" onClick={() => handleDelete(p.id, p.nome)}>Eliminar</button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" style={{ textAlign: 'center', color: '#6b7280', padding: '2rem' }}>
                    Nenhuma política definida.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              {editingId ? 'Editar Política RGPD' : 'Nova Política RGPD'}
            </div>
            <form className="modal-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Nome da Política</label>
                <input
                  type="text"
                  value={formData.nome}
                  onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                  placeholder="Ex: Limpeza de Logs"
                  required
                />
              </div>
              <div className="form-group">
                <label>Tipo de Dado</label>
                <select
                  value={formData.tipoDado}
                  onChange={(e) => setFormData({ ...formData, tipoDado: e.target.value })}
                >
                  <option value="BIOMETRICO">Biométrico</option>
                  <option value="LOCALIZACAO">Localização</option>
                  <option value="CONTACTO">Contacto</option>
                  <option value="TRANSACIONAL">Transacional</option>
                </select>
              </div>
              <div className="form-group">
                <label>Tempo de Retenção (Dias)</label>
                <input
                  type="number"
                  value={formData.tempoRetencao}
                  onChange={(e) => setFormData({ ...formData, tempoRetencao: parseInt(e.target.value) })}
                  min="1"
                  required
                />
              </div>
              <div className="form-group">
                <label>Descrição</label>
                <input
                  type="text"
                  value={formData.descricao}
                  onChange={(e) => setFormData({ ...formData, descricao: e.target.value })}
                  placeholder="Breve descrição da política"
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>Cancelar</button>
                <button type="submit" className="btn-primary">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default GestaoRGPD;
